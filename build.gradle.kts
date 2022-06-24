plugins{
    id("io.gitlab.arturbosch.detekt") version "1.20.0"
    id("org.jetbrains.dokka") version "1.5.0"
    id("org.sonarqube") version "3.2.0"
    id("jacoco")
}

buildscript {
    val compose_version by extra("1.1.0")
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.6.10")
        classpath("com.android.tools.build:gradle:7.0.0")
        classpath("com.vanniktech:gradle-maven-publish-plugin:0.15.1")
        classpath ("de.mannodermaus.gradle.plugins:android-junit5:1.7.1.1")
    }
}

allprojects {
    apply("$rootDir/detekt.gradle")
    apply(plugin = "org.jetbrains.dokka")
    apply(plugin = "jacoco")

    repositories {
        mavenLocal()
        google()
        mavenCentral()
    }

    afterEvaluate {
        apply("$rootDir/gradle/report/sonarqube.gradle")
        apply("$rootDir/gradle/report/jacoco.gradle")
    }
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}

apply("$rootDir/gradle/report/jacoco-merge.gradle")