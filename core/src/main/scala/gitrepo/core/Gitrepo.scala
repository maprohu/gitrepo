package gitrepo.core

import java.io.{FileOutputStream, File}
import java.nio.file.{StandardCopyOption, Files}

import akka.actor.{ActorSystem, PoisonPill}
import akka.stream._
import akka.stream.scaladsl.{Sink, Flow, Source, StreamConverters}
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.internal.storage.file.FileRepository
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.eclipse.jgit.transport.{CredentialsProvider, UsernamePasswordCredentialsProvider}
import akka.http.scaladsl.server.Directives._
import sbt.io.Path._
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.collection.JavaConversions._

/**
  * Created by pappmar on 07/01/2016.
  */
class Gitrepo(
  uri: String,
  credentials: CredentialsProvider,
  workDir : File
)(
  implicit actorSystem: ActorSystem,
  actorMaterializer: ActorMaterializer
) {
  import actorSystem.dispatcher

  val git = if (!workDir.exists()) {
    actorSystem.log.info(s"Cloning ${uri}")

    Git.cloneRepository()
      .setBare(false)
      .setURI(uri)
      .setDirectory(workDir)
      .setCredentialsProvider(credentials)
      .call()
  } else {
    actorSystem.log.info(s"Pulling ${uri}")

    val repository = new FileRepositoryBuilder()
      .setGitDir(workDir / ".git")
      .readEnvironment()
      .findGitDir()
      .build()
    val git = new Git(repository)
    val pullResult = git.pull().setCredentialsProvider(credentials).call()
    actorSystem.log.info(s"pullresult ${pullResult.getFetchResult.getMessages} - ${pullResult}")
    git
  }

  def push(message: String) = {
    actorSystem.log.info(s"performing push")
    git.add().addFilepattern(".").call()
    git.add().addFilepattern(".").setUpdate(true).call()
    git.commit().setMessage(s"${message} file(s) uploaded").call()
    val pushResult = git.push().setCredentialsProvider(credentials).call()
    actorSystem.log.info(s"pushresult ${pushResult.map(_.getRemoteUpdates)}")
  }

  case class Upload(
    target: File,
    content: File
  )

  val processor = Flow[Seq[Upload]]
    .mapAsync(1){ files => Future({
      actorSystem.log.info(s"processing ${files.size} files ${files.map(_.target)}")
      files.foreach { case Upload(target, content) =>
        target.getParentFile.mkdirs()
        Files.move(content.toPath, target.toPath, StandardCopyOption.REPLACE_EXISTING)

        target.getParentFile /
      }
      push(s"${files.size} file(s) uploaded")

    }).andThen({ case result =>
      actorSystem.log.info(s"${result} procesing ${files.map(_.target)}")
      result
    })}.withAttributes(Attributes.inputBuffer(initial = 1, max = 1))

  val committerFlow = Flow[Upload]
    .conflate(seed = Seq(_))((uploads, newUpload) => newUpload +: uploads)
    .via(processor)
    .to(Sink.ignore)

  val committer = committerFlow
    .runWith(
      Source.actorRef(1000, OverflowStrategy.fail)
    )

  push("gitrepo startup")
  actorSystem.log.info(s"Working in ${workDir.absolutePath}")

  val route =
    put {
      path(Segment / Segments) { case (repo, groupList :+ artifactId :+ version :+ fileName) =>
        extractMaterializer { materializer =>
          extractRequest { req =>
            val target = workDir / repo / groupList.mkString("/") / artifactId / version / fileName

            val content = Files.createTempFile("gitrepo", "upload").toFile

            val sink = StreamConverters.fromOutputStream(() => new FileOutputStream(content))

            val result = req.entity.getDataBytes()
              .runWith(sink, materializer)

            onSuccess(result) { _ =>
              committer ! Upload(target, content)
              actorSystem.log.info(s"received ${target}")
              complete("OK")
            }
          }
        }
      }
    } ~
    logRequestResult("GET", akka.event.Logging.InfoLevel) {
      getFromBrowseableDirectory(workDir.absolutePath)
    }

  def shutdown() = {
    committer ! PoisonPill
    // TODO do not delete before all tasks finished (watch committer termination)
    sbt.io.IO.delete(workDir)
  }

}
