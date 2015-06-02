name := """dynodb"""

version := "1.0.0"

sbtVersion := "0.13.8"

libraryDependencies ++= Seq("com.typesafe.slick" %% "slick" % "3.0.0",
							"com.zaxxer" % "HikariCP" % "2.3.8",
							"mysql" % "mysql-connector-java" % "5.1.35",
                            "com.typesafe.akka" %% "akka-actor" % "2.3.8",
							"com.typesafe.akka" %% "akka-cluster" % "2.3.8",
							"com.typesafe.akka" %% "akka-remote" % "2.3.8",
                            "com.typesafe.akka" %% "akka-contrib" % "2.3.8",
                            "com.typesafe.akka" %% "akka-testkit" % "2.3.8")