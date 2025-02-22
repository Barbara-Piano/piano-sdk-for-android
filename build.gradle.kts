// Top-level build file where you can add configuration options common to all sub-projects/modules.
import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask

plugins {
    id(Plugins.versions) version Versions.versionsPlugin
    id(Plugins.dokka) version Versions.dokkaPlugin
    id(Plugins.ktlint) version Versions.ktlintPlugin
    id(Plugins.publish) version Versions.publishPlugin
}

buildscript {
    repositories {
        google()
        mavenCentral()
    }

    dependencies {
        classpath(Plugins.androidTools)
        classpath(kotlin(Plugins.kotlin, Versions.kotlin))
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
        jcenter() {
            content {
                // https://github.com/Kotlin/dokka/issues/41
                includeModule("org.jetbrains.kotlinx", "kotlinx-html-jvm")
                // https://youtrack.jetbrains.com/issue/IDEA-261387 + https://issuetracker.google.com/issues/109894262 (wait for AGP 4.2.0)
                includeModule("org.jetbrains.trove4j", "trove4j")
            }
        }
    }
}

val GROUP: String by project
val VERSION_NAME: String by project

group = GROUP
version = VERSION_NAME

// We use okhttp 3.12.* and retrofit 2.6.*, because okhttp 3.13+ requires API 21 and retrofit 2.7+ requires API 21
val maxSupportedSquareLib = mapOf(
    "com.squareup.okhttp3" to arrayOf(3, 12),
    "com.squareup.retrofit2" to arrayOf(2, 6)
)

fun isNonStable(version: String): Boolean {
    val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.toUpperCase().contains(it) }
    val regex = "^[0-9,.v-]+(-r)?$".toRegex()
    val isStable = stableKeyword || regex.matches(version)
    return !isStable
}

fun isNotSupportedSquareLib(group: String, version: String): Boolean =
    maxSupportedSquareLib.any { (key, value) ->
        group == key && version.split("-").first().split(".")
            .map { it.toInt() }
            .zip(value)
            .any { it.first > it.second }
    }

tasks {
    named<DependencyUpdatesTask>("dependencyUpdates") {
        rejectVersionIf {
            isNotSupportedSquareLib(candidate.group, candidate.version) ||
                isNonStable(candidate.version) && !isNonStable(currentVersion)
        }

        checkForGradleUpdate = true
        outputFormatter = "json"
        outputDir = "build/dependencyUpdates"
        reportfileName = "report"
    }
}
