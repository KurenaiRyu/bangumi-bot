plugins {
    kotlin("jvm") version "2.1.10"
    kotlin("plugin.serialization") version "2.1.10"
}

group = "moe.kurenai.bot"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":common"))

    implementation(libs.kotlinx.coroutines)
    implementation(libs.kotlinx.json)
    implementation(libs.bundles.ktorClient)

    //qrcode
    implementation(libs.bundles.zxing)

    //logging
    implementation(libs.bundles.log)
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
