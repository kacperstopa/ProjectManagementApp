val Http4sVersion = "0.21.4"
val CirceVersion = "0.13.0"
val Specs2Version = "4.9.3"
val LogbackVersion = "1.2.3"
val DoobieVersion = "0.8.8"
val PureConfigVersion = "0.12.3"
val Http4sJwtAuthVersion = "0.0.5"
val ScalaTestVersion = "3.2.0"

lazy val root = (project in file("."))
  .settings(
    organization := "com.kstopa",
    name := "projectmanagement",
    version := "0.0.1",
    scalaVersion := "2.13.1",
    libraryDependencies ++= Seq(
      "org.http4s"      %% "http4s-blaze-server" % Http4sVersion,
      "org.http4s"      %% "http4s-blaze-client" % Http4sVersion,
      "org.http4s"      %% "http4s-circe"        % Http4sVersion,
      "org.http4s"      %% "http4s-dsl"          % Http4sVersion,
      "io.circe"        %% "circe-generic"       % CirceVersion,
      "io.circe" %% "circe-parser" % CirceVersion,
      "org.tpolecat"    %% "doobie-core"            % DoobieVersion,
      "org.tpolecat"    %% "doobie-postgres"        % DoobieVersion,
      "org.tpolecat"    %% "doobie-specs2"          % DoobieVersion,
      "org.tpolecat" %% "doobie-scalatest" % DoobieVersion % "test",
      "org.tpolecat"    %% "doobie-hikari"        % DoobieVersion,
      "com.github.pureconfig" %% "pureconfig" % PureConfigVersion,
      "com.github.pureconfig" %% "pureconfig-cats-effect" % PureConfigVersion,
      "org.specs2"      %% "specs2-core"         % Specs2Version % "test",
      "ch.qos.logback"  %  "logback-classic"     % LogbackVersion,
      "dev.profunktor" %% "http4s-jwt-auth" % Http4sJwtAuthVersion,
      "org.scalatest" %% "scalatest" % ScalaTestVersion,
    ),
    addCompilerPlugin("org.typelevel" %% "kind-projector"     % "0.10.3"),
    addCompilerPlugin("com.olegpy"    %% "better-monadic-for" % "0.3.1"),
  )

enablePlugins(DockerComposePlugin)
enablePlugins(JavaAppPackaging)
dockerImageCreationTask := (publishLocal in Docker).value


scalacOptions ++= Seq(
  "-deprecation",
  "-encoding", "UTF-8",
  "-language:higherKinds",
  "-language:postfixOps",
  "-feature",
  "-Xfatal-warnings",
)
