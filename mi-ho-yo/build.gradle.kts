plugins {
    kotlin("jvm") version "2.1.10"
    kotlin("plugin.serialization") version "2.1.10"
}

group = "moe.kurenai.bot"
version = "unspecified"

repositories {
    mavenCentral()
}

object Versions {
    const val vertxVersion = "4.2.3"
    const val log4j = "2.20.0"
    const val ktor = "3.1.3"
    const val tdlight = "3.4.0+td.1.8.26"
}
dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("io.ktor:ktor-client-core:${Versions.ktor}")
    implementation("io.ktor:ktor-network-tls-certificates:${Versions.ktor}")
    implementation("io.ktor:ktor-client-okhttp:${Versions.ktor}")
    implementation("io.ktor:ktor-client-content-negotiation:${Versions.ktor}")
    implementation("io.ktor:ktor-client-logging:${Versions.ktor}")
    implementation("io.ktor:ktor-serialization-kotlinx-json:${Versions.ktor}")

    //qrcode
    implementation("com.google.zxing:core:3.5.2")
    implementation("com.google.zxing:javase:3.5.2")

    //logging
    implementation("org.slf4j:slf4j-api:2.0.6")
    implementation("org.apache.logging.log4j:log4j-core:${Versions.log4j}")
    implementation("org.apache.logging.log4j:log4j-api:${Versions.log4j}")
    implementation("org.apache.logging.log4j:log4j-slf4j2-impl:${Versions.log4j}")
    implementation("com.lmax:disruptor:3.4.4")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
