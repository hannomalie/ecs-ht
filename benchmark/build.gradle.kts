plugins {
    id("me.champeau.jmh") version "0.7.0"
    kotlin("jvm") version "1.8.0"
}

repositories {
    mavenCentral()
}
dependencies {
    jmh("org.openjdk.jmh:jmh-core:0.9")
    jmh("org.openjdk.jmh:jmh-generator-annprocess:0.9")
    jmh(project(":"))
}

tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile::class.java).configureEach {
    compilerOptions {
        freeCompilerArgs.addAll("-Xcontext-receivers")
    }
}