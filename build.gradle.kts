plugins {
    kotlin("jvm") version "1.9.24"
    `java-gradle-plugin`
    `kotlin-dsl`
    id("io.deepmedia.tools.deployer") version "0.18.0"
}

group = "dev.nextftc"
version = property("version") as String

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.dokka:dokka-gradle-plugin:2.0.0")
    implementation("io.deepmedia.tools.deployer:deployer:0.18.0")
}

kotlin {
    jvmToolchain(17)
}

gradlePlugin {
    plugins.create("publishing") {
        id = "dev.nextftc.publishing"
        implementationClass = "dev.nextftc.publishing.PublishingPlugin"
    }
}

deployer {
    projectInfo {
        name = "NextFTC Publishing"
        description = "The plugin used to publish NextFTC libraries."
        url = "https://github.com/NextFTC/Publishing"
        scm {
            fromGithub("NextFTC", "Publishing")
        }
        license("GNU General Public License, version 3", "https://www.gnu.org/licenses/gpl-3.0.html")
        developer("Davis Luxenberg", "davis.luxenberg@outlook.com", url = "https://github.com/BeepBot99")
    }

    signing {
        key = secret("MVN_GPG_KEY")
        password = secret("MVN_GPG_PASSWORD")
    }

    content {
        gradlePluginComponents {
            kotlinSources()
            emptyDocs()
        }
    }

    localSpec {
        release.version = "$version-LOCAL"
    }

    nexusSpec("snapshot") {
        release.version = "$version-SNAPSHOT"
        repositoryUrl = "https://central.sonatype.com/repository/maven-snapshots/"
        auth {
            user = secret("SONATYPE_USERNAME")
            password = secret("SONATYPE_PASSWORD")
        }
    }

    centralPortalSpec {
        auth {
            user = secret("SONATYPE_USERNAME")
            password = secret("SONATYPE_PASSWORD")
        }
        allowMavenCentralSync = (property("automaticMavenCentralSync") as String).toBoolean()
    }
}