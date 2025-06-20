plugins {
    kotlin("jvm")
}

group = "moe.kurenai.bot"
version = "unspecified"

repositories {
    mavenCentral()
}

dependencies {
    api(libs.kotlinx.coroutines)
    api(libs.kotlinx.json)
    api(libs.slf4j.api)

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
