package dev.nextftc.publishing

import io.deepmedia.tools.deployer.DeployerExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.property
import org.gradle.kotlin.dsl.register
import org.jetbrains.dokka.gradle.DokkaExtension
import org.jetbrains.dokka.gradle.engine.plugins.DokkaHtmlPluginParameters
import javax.inject.Inject

open class PublishingExtension @Inject constructor(objects: ObjectFactory, project: Project) {
    val displayName = objects.property<String>()
    val version = objects.property<String>().convention(project.provider { project.version.toString() })
    val group = objects.property<String>().convention(project.provider { project.group.toString() })
    val automaticMavenCentralSync = objects.property<Boolean>()
        .convention(
            project.providers.gradleProperty("dev.nextftc.publishing.automaticMavenCentralSync").map(String::toBoolean)
        )
    val logoPath = objects.property<String>().convention("assets/logo-icon.svg")
}

@Suppress("unused")
class PublishingPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.pluginManager.apply("org.jetbrains.dokka")
        project.pluginManager.apply("io.deepmedia.tools.deployer")

        val extension = project.extensions.create<PublishingExtension>("nextFTCPublishing")

        project.extensions.configure<DokkaExtension> {
            project.pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
                dokkaSourceSets.named("main") {
                    sourceRoots.from(project.file("src/main/kotlin"))

                    sourceLink {
                        localDirectory.set(project.file("src/main/kotlin"))
                        remoteUrl("https://github.com/NextFTC/NextFTC/blob/main/${project.name}/src/main/kotlin")
                        remoteLineSuffix.set("#L")
                    }
                }
            }

            moduleName.set(extension.displayName)

            pluginsConfiguration.named<DokkaHtmlPluginParameters>("html") {
                footerMessage.set("Copyright © 2025 NextFTC - Licensed under the GNU General Public License v3.0.")
                customAssets.from(extension.logoPath)
            }
        }

        val dokkaJar = project.tasks.register<Jar>("dokkaJar") {
            dependsOn(project.tasks.named("dokkaGenerate"))
            from(project.extensions.getByType<DokkaExtension>().basePublicationsDirectory.dir("html"))
            archiveClassifier.set("html-docs")
        }

        project.extensions.configure<DeployerExtension> {
            signing {
                key.set(secret("MVN_GPG_KEY"))
                password.set(secret("MVN_GPG_PASSWORD"))
            }

            content {
                project.pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
                    kotlinComponents {
                        kotlinSources()
                        docs(dokkaJar)
                    }
                }

                project.pluginManager.withPlugin("org.jetbrains.kotlin.android") {
                    androidComponents("release") {
                        kotlinSources()
                        docs(dokkaJar)
                    }
                }
            }
        }

        project.extensions.configure<DeployerExtension> {
            projectInfo {
                name.set(extension.displayName)
                groupId.set(extension.group)
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