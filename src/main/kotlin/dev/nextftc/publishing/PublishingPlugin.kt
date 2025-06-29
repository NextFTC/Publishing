package dev.nextftc.publishing

import io.deepmedia.tools.deployer.DeployerExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.property
import org.gradle.kotlin.dsl.register
import org.jetbrains.dokka.gradle.DokkaExtension
import javax.inject.Inject

open class PublishingExtension @Inject constructor(objects: ObjectFactory, project: Project) {
    val displayName = objects.property<String>()
    val version = objects.property<String>().convention(project.provider { project.version.toString() })
    val group = objects.property<String>().convention(project.provider { project.group.toString() })
    val automaticMavenCentralSync = objects.property<Boolean>()
        .convention(
            project.providers.gradleProperty("dev.nextftc.publishing.automaticMavenCentralSync").map(String::toBoolean)
        )
}

@Suppress("unused")
class PublishingPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.pluginManager.apply("org.jetbrains.dokka")
        project.pluginManager.apply("io.deepmedia.tools.deployer")

        project.extensions.configure<DokkaExtension> {
            dokkaSourceSets.named("main") {
                sourceRoots.from(project.file("src/main/kotlin"))

                sourceLink {
                    localDirectory.set(project.file("src/main/kotlin"))
                    remoteUrl("https://github.com/NextFTC/NextFTC/blob/main/${project.name}/src/main/kotlin")
                    remoteLineSuffix.set("#L")
                }
            }
        }

        val dokkaJar = project.tasks.register<Jar>("dokkaJar") {
            dependsOn(project.tasks.named("dokkaGenerate"))
            from(project.extensions.getByType<DokkaExtension>().basePublicationsDirectory.dir("html"))
            archiveClassifier.set("html-docs")
        }


        val extension = project.extensions.create<PublishingExtension>("nextFTCPublishing")

        project.afterEvaluate {
            extensions.configure<DeployerExtension> {
                projectInfo {
                    name.set(extension.displayName)
                    groupId.set(extension.group)
                }

                signing {
                    key.set(secret("MVN_GPG_KEY"))
                    password.set(secret("MVN_GPG_PASSWORD"))
                }

                content {
                    pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
                        kotlinComponents {
                            kotlinSources()
                            docs(dokkaJar)
                        }
                    }

                    pluginManager.withPlugin("org.jetbrains.kotlin.android") {
                        androidComponents("release") {
                            kotlinSources()
                            docs(dokkaJar)
                        }
                    }
                }

                localSpec {
                    release.version.set("${extension.version}-LOCAL")
                }

                nexusSpec("snapshot") {
                    release.version.set("${extension.version}-SNAPSHOT")
                    repositoryUrl.set("https://central.sonatype.com/repository/maven-snapshots/")
                    auth {
                        user.set(secret("SONATYPE_USERNAME"))
                        password.set(secret("SONATYPE_PASSWORD"))
                    }
                }

                centralPortalSpec {
                    release.version.set(extension.version)
                    auth {
                        user.set(secret("SONATYPE_USERNAME"))
                        password.set(secret("SONATYPE_PASSWORD"))
                    }
                    allowMavenCentralSync.set(extension.automaticMavenCentralSync)
                }
            }
        }
    }
}