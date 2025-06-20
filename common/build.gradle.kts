plugins {
    kotlin("jvm")
}

group = "moe.kurenai.bot"
version = "unspecified"

repositories {
    mavenCentral()
}

dependencies {
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    api("org.slf4j:slf4j-api:2.0.6")
    api("org.apache.logging.log4j:log4j-core:2.20.0")

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
