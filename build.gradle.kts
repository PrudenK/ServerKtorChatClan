plugins {
    kotlin("jvm") version "1.9.10"
    application
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.ktor:ktor-server-core-jvm:2.3.4")
    implementation("io.ktor:ktor-server-netty-jvm:2.3.4")
    implementation("io.ktor:ktor-server-websockets:2.3.4")
    implementation("ch.qos.logback:logback-classic:1.4.11")
    implementation("org.json:json:20231013")
}

application {
    mainClass.set("MainKt")
}

kotlin {
    jvmToolchain(17)
}
