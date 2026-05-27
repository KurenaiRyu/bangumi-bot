plugins {
    alias(libs.plugins.kotlin.jvm)
}

repositories {
    mavenCentral()
}

dependencies {
    api(libs.kotlinx.coroutines)
    api(libs.kotlinx.json)
    api(libs.slf4j.api)
    api(libs.ktor.client.logging)
    api(libs.okio)

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
