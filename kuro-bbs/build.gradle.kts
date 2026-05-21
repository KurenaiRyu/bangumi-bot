plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
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

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xcontext-parameters")
    }
}

tasks.test {
    useJUnitPlatform()
}
