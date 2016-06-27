resolvers += Resolver.url("bintray-giltgroupe-sbt-plugin-releases", url("http://dl.bintray.com/giltgroupe/sbt-plugin-releases"))(Resolver.ivyStylePatterns)

addSbtPlugin("me.lessis" % "bintray-sbt" % "0.3.0")

addSbtPlugin("com.gilt.sbt" % "sbt-alpn" % "0.0.5")

addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.1.1")
