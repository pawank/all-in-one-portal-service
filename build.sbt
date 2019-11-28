import com.lightbend.lagom.core.LagomVersion
import sbt.Keys.{scalaVersion, version}
import sbtcrossproject.CrossPlugin.autoImport.{CrossType, crossProject}

scalaVersion in ThisBuild := "2.12.9"

isDevMode in ThisBuild := true

lazy val commonSettings = Seq(
  organization := "com.github.all-in-one-portal-service",
  version := "0.1.1"
)

lazy val commonJsSettings = commonSettings ++ Seq(
  scalacOptions += "-P:scalajs:sjsDefinedByDefault"
)

val lagomjsVersion = s"0.1.1-${LagomVersion.current}"
val macwire        = "com.softwaremill.macwire" %% "macros" % "2.3.3" % Provided

val pac4jVersion = "3.6.1"
val lagomPac4j = "org.pac4j" %% "lagom-pac4j" % "2.0.0"
val pac4jHttp = "org.pac4j" % "pac4j-http" % pac4jVersion
val pac4jJwt = "org.pac4j" % "pac4j-jwt" % pac4jVersion
val nimbusJoseJwt = "com.nimbusds" % "nimbus-jose-jwt" % "6.0"

val tikaCore = "org.apache.tika" % "tika" % "1.22"
val tikaParsers = "org.apache.tika" % "tika-parsers" % "1.22"
val arangodbJava = "com.arangodb" % "arangodb-java-driver" % "6.4.1"
val ammoniteOpsLib = "com.lihaoyi" %% "ammonite-ops" % "1.8.1"
val scalaXmlLib = "org.scala-lang.modules" %% "scala-xml" % "1.0.6"

lazy val `portal-api` = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Full)
  .in(file("portal-api"))
  .settings(
    name := "portal-api",
    libraryDependencies ++= Seq("io.github.cquiroz" %% "scala-java-time" % "2.0.0-RC3")
  )
  .jvmSettings(commonSettings: _*)
  .jvmSettings(
    libraryDependencies ++= Seq(lagomScaladslApi)
  )
  .jsSettings(commonJsSettings: _*)
  .jsSettings(
    libraryDependencies ++= Seq(
      "com.github.mliarakos.lagomjs" %%% "lagomjs-scaladsl-api" % lagomjsVersion,
        "io.github.cquiroz" %%% "scala-java-time" % "2.0.0-RC3"
    )
  )

lazy val `utils-service` = project
  .in(file("utils-service"))
  .settings(commonSettings: _*)
  .settings(
    name := "utils-service",
    isDevMode in scalaJSPipeline := !sys.env.get("SCALAJS_PROD").isDefined,
    libraryDependencies ++= Seq(
      tikaCore,
      tikaParsers,
      ammoniteOpsLib,
      scalaXmlLib
    ),
  )

lazy val `portal-impl` = project
  .in(file("portal-impl"))
  .settings(commonSettings: _*)
  .settings(
    name := "portal-impl",
    //lagomServiceAddress := "0.0.0.0",
    isDevMode in scalaJSPipeline := !sys.env.get("SCALAJS_PROD").isDefined,
    libraryDependencies ++= Seq(filters,
      pac4jHttp,
      pac4jJwt,
      lagomPac4j,
      nimbusJoseJwt,
      macwire)
  )
  .enablePlugins(LagomScala)
  .dependsOn(`portal-api`.jvm,`utils-service`)

lazy val `client-js` = project
  .in(file("client-js"))
  .settings(commonJsSettings: _*)
  .settings(
    name := "client-js",
    isDevMode in scalaJSPipeline := !sys.env.get("SCALAJS_PROD").isDefined,
    libraryDependencies ++= Seq(
      "com.github.mliarakos.lagomjs" %%% "lagomjs-scaladsl-client" % lagomjsVersion,
      "com.lihaoyi"                  %%% "scalatags"               % "0.6.8",
      "org.scala-js"                 %%% "scalajs-dom"             % "0.9.7",
      "io.github.cquiroz" %%% "scala-java-time-tzdb" % "2.0.0-RC3_2019a"
    ),
    scalaJSUseMainModuleInitializer := true
  )
  .enablePlugins(ScalaJSPlugin, ScalaJSWeb)
  .dependsOn(`portal-api`.js)

lazy val `client-ui` = project
  .in(file("client-ui"))
  .settings(commonSettings: _*)
  .settings(
    name := "client-ui",
    libraryDependencies ++= Seq(
      "org.webjars" % "bootstrap" % "4.3.1",
      filters,
      macwire
    ),
    scalaJSProjects := Seq(`client-js`),
    pipelineStages in Assets := Seq(scalaJSPipeline)
  )
  .enablePlugins(PlayScala, LagomPlay)

lazy val `portal-service` = project
  .in(file("."))
  .settings(
    publish / skip := true,
    publishLocal / skip := true
  )
  .aggregate(
    `portal-api`.jvm,
    `portal-api`.js,
    `portal-impl`,
    `utils-service`,
    `client-js`,
    `client-ui`
  )

lagomServiceLocatorAddress in ThisBuild := "127.0.0.1"
lagomServiceLocatorPort in ThisBuild := 9008
lagomServiceGatewayPort in ThisBuild := 9010
// Implementation of the service gateway: "akka-http" (default) or "netty"
lagomServiceGatewayImpl in ThisBuild := "akka-http"

lagomCassandraPort in ThisBuild := 4000
lagomCassandraCleanOnStart in ThisBuild := false
lagomCassandraJvmOptions in ThisBuild := Seq("-Xms256m", "-Xmx1024m", "-Dcassandra.jmx.local.port=4099")
import scala.concurrent.duration._ // Mind that the import is needed.
lagomCassandraMaxBootWaitingTime in ThisBuild := 20.seconds
lagomCassandraEnabled in ThisBuild := true
lagomUnmanagedServices in ThisBuild := Map("cas_native" -> "tcp://localhost:4000")
lagomKafkaPort in ThisBuild := 9092
lagomKafkaZookeeperPort in ThisBuild := 2181
lagomKafkaJvmOptions in ThisBuild := Seq("-Xms256m", "-Xmx1024m") // these are actually the default jvm options
lagomKafkaEnabled in ThisBuild := true
lagomKafkaAddress in ThisBuild := "localhost:9092"

isDevMode in scalaJSPipeline := !sys.env.get("SCALAJS_PROD").isDefined
