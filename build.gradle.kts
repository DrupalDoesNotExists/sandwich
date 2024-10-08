plugins {
    java
    `java-library`
    `maven-publish`
}

group = "ru.dldnex.bundle"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.netty:netty-all:4.1.112.Final")
    implementation("org.jetbrains:annotations:24.1.0")
    implementation("it.unimi.dsi:fastutil:8.5.14")
    implementation(files("libs/paper-1.16.5.jar"))
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(16)
    }
}
