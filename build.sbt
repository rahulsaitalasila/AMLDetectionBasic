name := "AMLDetectionBasic"

version := "0.1"

scalaVersion := "2.13.17"

libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-core" % "3.4.1",
  "org.apache.spark" %% "spark-sql" % "3.4.1",
  "org.postgresql" % "postgresql" % "42.7.7",
)

libraryDependencies += "org.knowm.xchart" % "xchart" % "3.8.1"

