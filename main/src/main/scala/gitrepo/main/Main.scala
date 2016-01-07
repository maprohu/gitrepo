package gitrepo.main

import java.io.File
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer

import gitrepo.core.Gitrepo
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider


object Main extends App {
  val uri = args(0)
  val userName = args(1)
  val password = args(2)
  val dirPath = args(3)

  implicit val system = ActorSystem("my-system")
  implicit val materializer = ActorMaterializer()
  implicit val ec = system.dispatcher

  val dir = new File(dirPath)
  val gitrepo = new Gitrepo(
    uri,
    new UsernamePasswordCredentialsProvider(userName, password),
    dir
  )

  val route = gitrepo.route

  val bindingFuture = Http().bindAndHandle(route, "localhost", 38084).onComplete { result =>
    system.log.info(s"Binding ${result}")
  }

}
