package gitrepo.deploy

import java.io.File

import sbt._


/**
  * Created by pappmar on 18/04/2016.
  */
object Deploy {

  def main(args: Array[String]) {
    val log = ConsoleLogger()
    val ivyPaths = new IvyPaths(new File("target/ivy"), None)
    val ivyConfiguration =  new InlineIvyConfiguration(
      paths = ivyPaths,
      resolvers = Seq(
        Resolver.typesafeIvyRepo("releases")
      ),
      otherResolvers = Seq(),
      moduleConfigurations = Seq(),
      localOnly = false,
      lock = None,
      checksums = Seq(),
      resolutionCacheDir = None,
      updateOptions = UpdateOptions(),
      log = log
    )


    val ivySbt = new IvySbt(ivyConfiguration)

    val moduleInfo =
      ModuleInfo(
        nameFormal = "bla"
      )
    val moduleSettings = InlineConfigurationWithExcludes(
      ModuleID(
        organization = "bla",
        name = "bla",
        revision = "bla"
      ),
      moduleInfo,
      dependencies = Seq()
    )

    val module = new ivySbt.Module(moduleSettings)


    IvyActions.makePom(
      module,
      MakePomConfiguration(
        new File("target/generated-pom.xml"),
        moduleSettings.moduleInfo,
        allRepositories = false
      ),
      log
    )



  }

}
