plugins {
    `java-platform`
    `maven-publish`
    signing
}

val projGroupId: String by project
val projArtifactId: String by project
val projVersion: String by project
val projVcs: String by project
val projBranch: String by project
val orgName: String by project
val orgUrl: String by project
val developers: String by project

group = projGroupId
version = projVersion

val binPackingVersion: String by project
val binTagVersion: String by project
val poolingVersion: String by project
val timerVersion: String by project
val unifontVersion: String by project

data class Artifact(val artifact: String, val version: String)

val BIN_PACKING = Artifact("bin-packing", binPackingVersion)
val BIN_TAG = Artifact("bin-tag", binTagVersion)
val POOLING = Artifact("pooling", poolingVersion)
val TIMER = Artifact("timer", timerVersion)
val UNIFONT = Artifact("unifont", unifontVersion)

val utilities = arrayOf(
    BIN_PACKING,
    BIN_TAG,
    POOLING,
    TIMER,
    UNIFONT
)

repositories {
    mavenCentral()
}

publishing {
    publications {
        fun MavenPom.setupPom(pomName: String, pomDescription: String, pomPackaging: String) {
            name.set(pomName)
            description.set(pomDescription)
            url.set("https://github.com/$projVcs")
            packaging = pomPackaging
            licenses {
                license {
                    name.set("MIT")
                    url.set("https://raw.githubusercontent.com/$projVcs/$projBranch/LICENSE")
                }
            }
            organization {
                name.set(orgName)
                url.set(orgUrl)
            }
            developers {
                developers.split(',')
                    .forEach { id1 ->
                        developer {
                            id.set(id1)
                        }
                    }
            }
            scm {
                connection.set("scm:git:https://github.com/${projVcs}.git")
                developerConnection.set("scm:git:https://github.com/${projVcs}.git")
                url.set("https://github.com/${projVcs}.git")
            }
        }

        create<MavenPublication>("utilitiesBOM") {
            from(components["javaPlatform"])
            artifactId = projArtifactId

            pom {
                setupPom("Utilities", "Utilities Bill of Materials.", "pom")

                withXml {
                    asElement().getElementsByTagName("dependencyManagement").item(0).apply {
                        asElement().getElementsByTagName("dependencies").item(0).apply {
                            utilities.forEach {
                                ownerDocument.createElement("dependency").also(::appendChild).apply {
                                    appendChild(
                                        ownerDocument.createElement("groupId").also(::appendChild)
                                            .apply { textContent = "io.github.over-run" })
                                    appendChild(
                                        ownerDocument.createElement("artifactId").also(::appendChild)
                                            .apply { textContent = it.artifact })
                                    appendChild(
                                        ownerDocument.createElement("version").also(::appendChild)
                                            .apply { textContent = it.version })
                                }
                            }
                        }
                    }

                    asNode()
                }
            }
        }
    }

    // You have to add `OSSRH_USERNAME`, `OSSRH_PASSWORD`, `signing.keyId`,
    // `signing.password` and `signing.secretKeyRingFile` to
    // GRADLE_USER_HOME/gradle.properties
    repositories {
        maven {
            name = "OSSRH"
            credentials {
                username = project.findProperty("OSSRH_USERNAME").toString()
                password = project.findProperty("OSSRH_PASSWORD").toString()
            }
            url = uri(
                if (projVersion.endsWith("-SNAPSHOT"))
                    "https://s01.oss.sonatype.org/content/repositories/snapshots/"
                else "https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/"
            )
        }
    }
}

signing {
    if (!projVersion.endsWith("-SNAPSHOT") && System.getProperty("gpg.signing", "true").toBoolean()) {
        sign(publishing.publications)
    }
}

dependencies {
    constraints {
        utilities.forEach {
            api("io.github.over-run:${it.artifact}:${it.version}")
        }
    }
}
