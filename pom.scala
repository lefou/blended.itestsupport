import org.sonatype.maven.polyglot.scala.model._
import scala.collection.immutable.Seq

//#include build-versions.scala
//#include build-dependencies.scala
//#include build-plugins.scala
//#include build-common.scala

BlendedModel(
  gav = Blended.itestSupport,
  packaging = "jar",
  description = """Define an integration test API for collaborating blended container(s) using docker as a runtime 
    for the container(s) under test and an Akka based Camel framework to perform the integration tests 
    as pure blackbox tests. Container(s) may be prestarted and discovered (for execution speed) or 
    started by the integration test (for reproducability).""",
  dependencies = Seq(
    Deps.scalaLib % "provided",
    Blended.akka,
    Blended.jolokia,
    Blended.testSupport,
    Blended.jmsUtils,
    Blended.utilLogging,
    Deps.dockerJava,
    Deps.akkaCamel,
    Deps.typesafeConfig,
    Deps.junit,
    Deps.commonsExec,
    Deps.camelCore,
    Deps.camelJms,
    Deps.slf4j,
    Deps.geronimoJms11Spec,
    Deps.commonsCompress,
    Deps.jsr305,
    Deps.akkaSlf4j % "test",
    Deps.mockitoAll % "test",
    Deps.activeMqBroker % "test",
    Deps.activeMqKahadbStore % "test",
    Deps.logbackCore % "test",
    Deps.logbackClassic % "test",
    Deps.jolokiaJvmAgent % "runtime"
  ),
  plugins = Seq(
    Plugin(
      gav = Plugins.dependency,
      executions = Seq(
        Execution(
          id = "extract-blended-container",
          phase = "process-resources",
          goals = Seq(
            "copy-dependencies"
          ),
          configuration = Config(
            includeClassifiers = "agent",
            outputDirectory = "${project.build.directory}/jolokia"
          )
        )
      )
    ),
    scalaCompilerPlugin,
    Plugin(
      scalatestMavenPlugin.gav,
      executions = Seq(
        scalatestExecution
      ),
      configuration = new Config(
        scalatestConfiguration.elements ++
          Config(
            argLine = "-javaagent:${project.build.directory}/jolokia/jolokia-jvm-" + BlendedVersions.jolokiaVersion + "-agent.jar=port=7777,host=localhost"
          ).elements
      )
    )
  )
)
