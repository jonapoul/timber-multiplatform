@file:Suppress("UnstableApiUsage")

import blueprint.core.intProperty
import blueprint.core.javaVersion
import blueprint.recipes.DetektAll
import blueprint.recipes.detektBlueprint
import blueprint.recipes.spotlessBlueprint
import com.vanniktech.maven.publish.MavenPublishPlugin
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    repositories {
        mavenCentral()
        maven("https://jitpack.io")
    }
    dependencies {
        classpath(libs.plugin.blueprint.core)
        classpath(libs.plugin.blueprint.recipes)
        classpath(libs.plugin.publish)
    }
}

plugins {
    alias(libs.plugins.agp.lib)
    alias(libs.plugins.androidCacheFix)
    alias(libs.plugins.dependencyAnalysis)
    alias(libs.plugins.dependencyVersions)
    alias(libs.plugins.detekt)
    alias(libs.plugins.dokka)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kover)
//    alias(libs.plugins.publish)
    alias(libs.plugins.spotless)
}

pluginManager.apply(MavenPublishPlugin::class.java)

kotlin {
    jvm()
    android()

    sourceSets {
        commonMain {
            dependencies {
                api(libs.kotlin.stdlib)
                implementation(libs.jetbrains.annotations)
            }
        }

        commonTest {
            dependencies {
                implementation(libs.jetbrains.annotations)
                implementation(libs.test.junit)
                implementation(libs.test.kotlin.common)
                implementation(libs.test.kotlin.junit)
                implementation(libs.test.robolectric)
            }
        }
    }
}

tasks.withType<KotlinCompile> {
    compilerOptions {
        freeCompilerArgs.addAll(
            "-Xjvm-default=all-compatibility",
            "-opt-in=kotlin.RequiresOptIn",
        )
    }
}

android {
    namespace = "timber.log"
    compileSdk = intProperty(key = "compileSdk")

    defaultConfig {
        minSdk = intProperty(key = "minSdk")
        consumerProguardFiles("consumer-proguard-rules.pro")
    }

    compileOptions {
        val version = javaVersion()
        sourceCompatibility = version
        targetCompatibility = version
    }

    lint {
        textReport = true
    }
}

detektBlueprint(detektAllConfig = DetektAll.Apply(ignoreRelease = true))
spotlessBlueprint()

tasks.withType<Test> {
    testLogging {
        exceptionFormat = TestExceptionFormat.FULL
        showCauses = true
        showExceptions = true
        showStackTraces = true
        showStandardStreams = true
    }
}
