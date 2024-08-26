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
    implementation(files("libs/paper-1.16.5.jar"))
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(16)
    }
}
