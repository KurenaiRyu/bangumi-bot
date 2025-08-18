plugins {
    application
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.lombok)
    alias(libs.plugins.lombok)
}

group = "moe.kurenai.bot"
version = "1.0-SNAPSHOT"

dependencies {
    implementation(project(":bangumi-api"))
    implementation(project(":common"))

    implementation(platform(libs.kotlin.bom))
    implementation(libs.kotlin.gradle)

    implementation(libs.kotlinx.coroutines)
    implementation(libs.kotlinx.json)
    implementation(libs.bundles.ktorClient)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.tls)

    implementation(libs.serialization.yaml)

    //cache
    implementation(libs.caffeine)

    implementation(libs.directory.watcher)
    implementation(libs.jsoup)
    implementation(libs.apache.commons.pool2)

    //tdlib
    implementation(platform(libs.tdlight.bom))
    implementation(libs.tdlight)
    val hostOs = System.getProperty("os.name")
    val isWin = hostOs.startsWith("Windows")
    val classifier = when {
        hostOs == "Linux" -> "linux_amd64_gnu_ssl1"
        isWin -> "windows_amd64"
        else -> throw GradleException("[$hostOs] is not support!")
    }
    implementation(group = "it.tdlight", name = "tdlight-natives", classifier = classifier)
    implementation(group = "it.tdlight", name = "tdlight-natives", classifier = "linux_amd64_gnu_ssl1")
    //qrcode
    implementation(libs.bundles.zxing)

    //logging
    implementation(libs.bundles.log)

    // queue
    implementation(libs.diruptor)

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnit()
}

val main = "$group.BgmApplicationKt"

application {
    mainClass.set(main)
}

tasks.register<Sync>("syncLib") {
    from(configurations.compileClasspath)
    into("${layout.buildDirectory.get()}/libs/lib")
}

tasks.jar {
    dependsOn("syncLib")
    exclude("**/*.jar")
    manifest {
        attributes["Manifest-Version"] = "1.0"
        attributes["Multi-Release"] = "true"
        attributes["Main-Class"] = main
        attributes["Class-Path"] = configurations.runtimeClasspath.get().files.joinToString(" ") { "lib/${it.name}" }
    }
    archiveFileName.set("${rootProject.name}.jar")
}

kotlin {
    compilerOptions {
        freeCompilerArgs = listOf(
            "-Xjsr305=strict",
            "-Xcontext-parameters"
        )
        javaParameters = true
    }
}
