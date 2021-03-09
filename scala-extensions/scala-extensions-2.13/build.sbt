lazy val `scala-extensions-2-13` = project
  .in(file("."))
  .settings(
    resolvers += Resolver.mavenLocal,
    scalaVersion := "2.13.5",
    libraryDependencies ++= Seq(
      "com.github.spullara.mustache.java" % "compiler" % "0.9.7",
      "junit" % "junit" % "4.8.2" % "test",

      "com.novocode" % "junit-interface" % "0.11" % "test"
        exclude("junit", "junit-dep")

    ),
    scalacOptions ++= Seq(
      "-feature",
      "-deprecation",
      "-unchecked",
      "-language:reflectiveCalls"
    )
  )
