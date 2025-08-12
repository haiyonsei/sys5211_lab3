//scalaVersion := "2.13.10"
//scalaVersion := "2.12.10"
scalaVersion := "2.12.15"

scalacOptions ++= Seq(
  "-deprecation:false",
  "-feature",
  "-unchecked",
  //"-Xfatal-warnings",
  "-language:reflectiveCalls",
)

//val chiselVersion = "3.5.1"
val chiselVersion = "3.5.3"
addCompilerPlugin("edu.berkeley.cs" %% "chisel3-plugin" % chiselVersion cross CrossVersion.full)
libraryDependencies += "edu.berkeley.cs" %% "chisel3" % chiselVersion
//libraryDependencies += "edu.berkeley.cs" %% "chiseltest" % "0.6.1"
libraryDependencies += "edu.berkeley.cs" %% "chiseltest" % "0.5.3"


// Rocket-chip dependencies (subsumes making RC a RootProject)
/*
lazy val hardfloat  = (project in rocketChipDir / "hardfloat")
  .settings(chiselSettings)
  .dependsOn(midasTargetUtils)
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= Seq(
      "org.scala-lang" % "scala-reflect" % scalaVersion.value,
      "org.json4s" %% "json4s-jackson" % "3.6.1",
      "org.scalatest" %% "scalatest" % "3.2.0" % "test"
    )
  )

lazy val rocketMacros  = (project in rocketChipDir / "macros")
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= Seq(
      "org.scala-lang" % "scala-reflect" % scalaVersion.value,
      "org.json4s" %% "json4s-jackson" % "3.6.1",
      "org.scalatest" %% "scalatest" % "3.2.0" % "test"
    )
  )

lazy val rocketConfig = (project in rocketChipDir / "api-config-chipsalliance/build-rules/sbt")
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= Seq(
      "org.scala-lang" % "scala-reflect" % scalaVersion.value,
      "org.json4s" %% "json4s-jackson" % "3.6.1",
      "org.scalatest" %% "scalatest" % "3.2.0" % "test"
    )
  )

lazy val rocketchip = freshProject("rocketchip", rocketChipDir)
  .dependsOn(hardfloat, rocketMacros, rocketConfig)
  .settings(commonSettings)
  .settings(chiselSettings)
  .settings(
    libraryDependencies ++= Seq(
      "org.scala-lang" % "scala-reflect" % scalaVersion.value,
      "org.json4s" %% "json4s-jackson" % "3.6.1",
      "org.scalatest" %% "scalatest" % "3.2.0" % "test"
    )
  )
  .settings( // Settings for scalafix
    semanticdbEnabled := true,
    semanticdbVersion := scalafixSemanticdb.revision,
    scalacOptions += "-Ywarn-unused-import"
  )
lazy val rocketLibDeps = (rocketchip / Keys.libraryDependencies)
*/



//resolvers += Resolver.mavenLocal // 혹시 필요하면(보통은 기본 포함)

//libraryDependencies ++= Seq(
 // "edu.berkeley.cs" %% "rocketchip" % "1.6",
  // rocket-chip이 끌고오는 것들은 POM으로 자동 해결
  // chiseltest를 쓰면 Chipyard와 동일 버전으로 맞추세요.
  //"edu.berkeley.cs" %% "chiseltest" % "0.6.1" % Test // 예시
//)

// 가능하면 scalaVersion도 rocketchip과 동일하게:
//ThisBuild / scalaVersion := (ThisProjectRef(file("/hai/home/jyc/haf/chipyard/generators/rocket-chip"), "rocketchip") / scalaVersion).value


/*
// Point to your Chipyard checkout
lazy val rocketchip = ProjectRef(file("/hai/projects/haf/202507/chipyard/generators/rocket-chip"), "rocketchip")
lazy val root = (project in file("."))
  .dependsOn(rocketchip)
  .settings(
    scalaVersion := (rocketchip / scalaVersion).value,
    libraryDependencies += "org.chipsalliance" %% "chiseltest" % "0.6.0" % Test
  )
*/
