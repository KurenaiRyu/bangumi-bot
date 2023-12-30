import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    application
    kotlin("jvm") version "1.9.10"
    kotlin("plugin.serialization") version "1.9.10"
    kotlin("plugin.lombok") version "1.9.10"
    id("io.freefair.lombok") version "5.3.0"
//    id("org.graalvm.buildtools.native") version "0.9.20"
}

group = "moe.kurenai.bot"
version = "1.0-SNAPSHOT"

repositories {
    maven("https://mvn.mchv.eu/repository/mchv/") {
        content {
            includeGroup("it.tdlight")
        }
    }
    mavenCentral()
    google()
    mavenLocal {
        content {
            includeGroup("com.github.kurenairyu")
        }
    }
}

object Versions {
    const val vertxVersion = "4.2.3"
    const val log4j = "2.20.0"
    const val ktor = "2.3.5"
    const val tdlight = "3.4.0+td.1.8.26"
}
dependencies {
    implementation(project(":sdk"))

    implementation(platform("org.jetbrains.kotlin:kotlin-bom:1.9.10"))
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0")
    implementation("io.ktor:ktor-client-core:${Versions.ktor}")
    implementation("io.ktor:ktor-network-tls-certificates:${Versions.ktor}")
    implementation("io.ktor:ktor-client-okhttp:${Versions.ktor}")
    implementation("io.ktor:ktor-server-netty:${Versions.ktor}")
    implementation("io.ktor:ktor-server-cio:${Versions.ktor}")
    implementation("io.ktor:ktor-serialization-kotlinx-json:${Versions.ktor}")

    //serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0")
    implementation("net.mamoe.yamlkt:yamlkt-jvm:0.12.0")

    //cache
    implementation("com.sksamuel.aedile:aedile-core:1.2.0")

    implementation("org.jsoup:jsoup:1.15.3")

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

//kotlin {
//    sourceSets.all {
//        languageSettings {
//            languageVersion = "2.0"
//        }
//    }
//}

tasks.test {
    useJUnit()
}

val main = "$group.BgmApplicationKt"

application {
    mainClass.set(main)
}

tasks.register<Delete>("clearLib") {
    delete("$buildDir/libs/lib")
}

tasks.register<Copy>("copyLib") {
    from(configurations.compileClasspath)
    into("$buildDir/libs/lib")
}

tasks.jar {
    dependsOn("clearLib")
    dependsOn("copyLib")
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

//graalvmNative {
//    binaries {
//        all {
//            resources.autodetect()
//        }
//        named("main") {
//            javaLauncher.set(javaToolchains.launcherFor {
//                languageVersion.set(JavaLanguageVersion.of(17))
//                vendor.set(JvmVendorSpec.matching("GraalVM Community"))
//            })
//            debug.set(true)
//            verbose.set(true)
//            richOutput.set(true)
//
//            buildArgs.add(
//                "--initialize-at-build-time=" +
//                    "io.ktor," +
//                    "kotlin," +
//                    "kotlinx," +
//                    "com.fasterxml.jackson," +
//                    "org.yaml.snakeyaml," +
//                    "net.mamoe.yamlkt," +
//                    "ch.qos.logback," +
//                    "org.slf4j," +
//                    "com.github.benmanes.caffeine"
//            )
//
//            buildArgs.add(
//                "--initialize-at-run-time=" +
//                    "io.netty.buffer.AbstractByteBufAllocator," +
//                    "io.netty.channel.epoll.Epoll," +
//                    "io.netty.channel.epoll.EpollEventLoop," +
//                    "io.netty.channel.epoll.EpollEventArray," +
//                    "io.netty.channel.epoll.Native," +
//                    "io.netty.channel.DefaultFileRegion," +
//                    "io.netty.channel.unix.Errors"
//            )
////            buildArgs.add("--initialize-at-run-time=io.netty.buffer.AbstractByteBufAllocator")
////            buildArgs.add("--initialize-at-run-time=io.netty.channel.DefaultFileRegion")
////            buildArgs.add("--initialize-at-run-time=io.netty.channel.epoll.Native")
////            buildArgs.add("--initialize-at-run-time=io.netty.channel.epoll.Epoll")
////            buildArgs.add("--initialize-at-run-time=io.netty.channel.epoll.EpollEventLoop")
////            buildArgs.add("--initialize-at-run-time=io.netty.channel.epoll.EpollEventArray")
////            buildArgs.add("--initialize-at-run-time=io.netty.channel.kqueue.KQueue")
////            buildArgs.add("--initialize-at-run-time=io.netty.channel.kqueue.KQueueEventLoop")
////            buildArgs.add("--initialize-at-run-time=io.netty.channel.kqueue.KQueueEventArray")
////            buildArgs.add("--initialize-at-run-time=io.netty.channel.kqueue.Native")
////            buildArgs.add("--initialize-at-run-time=io.netty.channel.unix.Limits")
////            buildArgs.add("--initialize-at-run-time=io.netty.channel.unix.Errors")
////            buildArgs.add("--initialize-at-run-time=io.netty.channel.unix.IovArray")
//
////            buildArgs.add("--trace-class-initialization=com.fasterxml.jackson.core.util.VersionUtil")
//
//            buildArgs.add("--enable-url-protocols=http")
//            buildArgs.add("-H:+InstallExitHandlers")
//            buildArgs.add("-H:+ReportExceptionStackTraces")
//            buildArgs.add("-H:-CheckToolchain")
//        }
//    }
//}
