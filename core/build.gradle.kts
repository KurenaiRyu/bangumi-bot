import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    application
    kotlin("jvm") version "2.1.10"
    kotlin("plugin.serialization") version "2.1.10"
    kotlin("plugin.lombok") version "2.1.10"
    id("io.freefair.lombok") version "5.3.0"
}

group = "moe.kurenai.bot"
version = "1.0-SNAPSHOT"

object Versions {
    const val vertxVersion = "4.2.3"
    const val log4j = "2.20.0"
    const val ktor = "3.1.0"
    const val tdlight = "3.4.0+td.1.8.26"
}
dependencies {
    implementation(project(":bangumi-api"))

    implementation(platform("org.jetbrains.kotlin:kotlin-bom:1.9.10"))
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0")
    implementation("io.ktor:ktor-client-core:${Versions.ktor}")
    implementation("io.ktor:ktor-network-tls-certificates:${Versions.ktor}")
    implementation("io.ktor:ktor-client-okhttp:${Versions.ktor}")
    implementation("io.ktor:ktor-server-netty:${Versions.ktor}")
    implementation("io.ktor:ktor-serialization-kotlinx-json:${Versions.ktor}")

    //serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0")
    implementation("net.mamoe.yamlkt:yamlkt-jvm:0.13.0")

    //cache
    implementation("com.sksamuel.aedile:aedile-core:1.2.0")

    implementation("org.jsoup:jsoup:1.15.3")
    implementation("org.apache.commons:commons-pool2:2.11.1")

    //tdlib
    implementation(platform("it.tdlight:tdlight-java-bom:${Versions.tdlight}"))
    implementation("it.tdlight:tdlight-java")
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
    implementation("com.google.zxing:core:3.5.1")

    //logging
    implementation("org.slf4j:slf4j-api:2.0.6")
    implementation("org.apache.logging.log4j:log4j-core:${Versions.log4j}")
    implementation("org.apache.logging.log4j:log4j-api:${Versions.log4j}")
    implementation("org.apache.logging.log4j:log4j-slf4j2-impl:${Versions.log4j}")
    implementation("com.lmax:disruptor:3.4.4")

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

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf(
            "-Xjsr305=strict",
        )
        javaParameters = true
    }
}
