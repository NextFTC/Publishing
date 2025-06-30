package dev.nextftc.publishing

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply

class MultiModulePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.subprojects {
            pluginManager.apply(PublishingPlugin::class)
        }

        project.tasks.register("deployCentralPortal") {
            group = "publishing"
            description = "Publishes all subprojects to Maven Central."
            dependsOn(project.subprojects.map { "${it.path}:deployCentralPortal" })
        }

        project.tasks.register("deployLocal") {
            group = "publishing"
            description = "Publishes all subprojects to Maven Local."
            dependsOn(project.subprojects.map { "${it.path}:deployLocal" })
        }

        project.tasks.register("deployNexusSnapshot") {
            group = "publishing"
            description = "Publishes all subprojects to Maven Central Snapshots."
            dependsOn(project.subprojects.map { "${it.path}:deployNexusSnapshot" })
        }
    }
}