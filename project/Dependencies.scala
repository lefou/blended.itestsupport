import sbt._

object Dependencies {

  private[this] val activeMqVersion = "5.15.3"
  private[this] val akkaVersion = "2.5.9"
  private[this] val blendedCoreVersion = "2.6.0-M1-SNAPSHOT"
  private[this] val dockerJavaVersion = "3.0.13"

  private[this] def akka(m: String) : ModuleID = "com.typesafe.akka" %% s"akka-${m}" % akkaVersion
  private[this] def blended(module: String) : ModuleID = "de.wayofquality.blended" %% module % blendedCoreVersion

  val activeMqBroker = "org.apache.activemq" % "activemq-broker" % activeMqVersion
  val activeMqKahadbStore = "org.apache.activemq" % "activemq-kahadb-store" % activeMqVersion

  val akkaActor = akka("actor")
  val akkaCamel = akka("camel")
  val akkaSlf4j = akka("slf4j")
  val akkaTestkit = akka("testkit")

  val blendedJmsUtils = blended("blended.jms.utils")
  val blendedJolokia = blended("blended.jolokia")
  val blendedUtilLogging = blended("blended.util.logging")
  val blendedTestsupport = blended("blended.testsupport")

  val commonsCompress = "org.apache.commons" % "commons-compress" % "1.13"

  val dockerJava = "com.github.docker-java" % "docker-java" % dockerJavaVersion

  val jolokiaJvm = "org.jolokia" % "jolokia-jvm" % "1.5.0"
  val jolokiaJvmAgent = jolokiaJvm.classifier("agent")

  val logbackCore = "ch.qos.logback" % "logback-core" % "1.2.3"
  val logbackClassic = "ch.qos.logback" % "logback-classic" % "1.2.3"

  val mockitoAll = "org.mockito" % "mockito-all" % "1.9.5"
}
