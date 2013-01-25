import AssemblyKeys._

name := "crawl-browser"

version := "0.1"


libraryDependencies += "junit" % "junit" % "4.8.1" % "test"

libraryDependencies += "org.apache.commons" % "commons-compress" % "1.4.1" withSources ()

libraryDependencies += "commons-io" % "commons-io" % "2.4"

libraryDependencies += "net.htmlparser.jericho" % "jericho-html" % "3.1" withSources ()

libraryDependencies += "org.jwat" % "jwat-warc" % "0.9.1"  withSources ()

libraryDependencies += "org.jwat" % "jwat-common" % "0.9.1"  withSources ()

libraryDependencies += "de.l3s.boilerpipe" % "boilerpipe" % "1.1.0"  withSources ()

libraryDependencies += "org.apache.lucene" % "lucene-core" % "4.0.0-BETA"

libraryDependencies += "org.apache.lucene" % "lucene-analyzers-common" % "4.0.0-BETA"

libraryDependencies += "com.google.code.gson" % "gson" % "2.2.2"

// Disabled since we're using the local version
// libraryDependencies += "net.sourceforge.argparse4j" % "argparse4j" % "0.2.2"  withSources ()





// Include only src/main/java in the compile configuration
// unmanagedSourceDirectories in Compile <<= Seq(javaSource in Compile).join

// Include only src/test/java in the test configuration
unmanagedSourceDirectories in Test <<= Seq(javaSource in Test).join

// Enable sbt-assembly
assemblySettings

assembleArtifact in packageScala := true

