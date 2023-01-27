import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
//    id("io.quarkus")
    kotlin("jvm") version "1.7.22"
    application
    id("org.graalvm.buildtools.native") version "0.9.19"
    kotlin("plugin.serialization") version "1.7.22"
}

group = "moe.kurenai.bot"
version = "1.0-SNAPSHOT"

repositories {
    mavenLocal()
    maven { url = uri("https://jitpack.io") }
    mavenCentral()
}

val vertxVersion = "4.2.3"
val log4j = "2.19.0"
val ktor = "2.1.2"
dependencies {
    implementation("com.github.kurenairyu:bangumi-sdk:0.0.1")
    implementation("moe.kurenai.tdlight", "td-light-sdk", "0.1.0-SNAPSHOT")
    implementation("moe.kurenai", "kt-telegram-bot", "2.1.8.1")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:1.6.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-slf4j:1.6.4")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.1")
    implementation("io.ktor:ktor-client-core:$ktor")
    implementation("io.ktor:ktor-server-cio:$ktor")
    implementation("io.ktor:ktor-server-call-logging:$ktor")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktor")

    //serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.1")
    implementation("net.mamoe.yamlkt:yamlkt-jvm:0.12.0")

    //cache
//    implementation("org.redisson:redisson:3.19.1")
//    implementation("com.github.ben-manes.caffeine:caffeine:3.1.2")
    implementation("com.sksamuel.aedile:aedile-core:1.2.0")

    implementation("org.jsoup:jsoup:1.15.3")

    implementation("org.reflections", "reflections", "0.10.2")


    //logging
//    implementation("org.apache.logging.log4j:log4j-core:$log4j")
//    implementation("org.apache.logging.log4j:log4j-slf4j-impl:$log4j")
//    implementation("com.lmax:disruptor:3.4.4")
    implementation("ch.qos.logback:logback-classic:1.2.11")

    testImplementation(kotlin("test"))
}

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

//tasks.jar {
//    dependsOn("clearLib")
//    dependsOn("copyLib")
//    exclude("**/*.jar")
//    manifest {
//        attributes["Manifest-Version"] = "1.0"
//        attributes["Multi-Release"] = "true"
//        attributes["Main-Class"] = main
//        attributes["Class-Path"] = configurations.runtimeClasspath.get().files.map { "lib/${it.name}" }.joinToString(" ")
//    }
//    archiveFileName.set("${rootProject.name}.jar")
//}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf(
            "-Xjsr305=strict",
        )
        jvmTarget = JavaVersion.VERSION_17.toString()
        javaParameters = true
    }
}

graalvmNative {
    binaries {
        all {
            resources.autodetect()
        }
        named("main") {
            javaLauncher.set(javaToolchains.launcherFor {
                languageVersion.set(JavaLanguageVersion.of(17))
                vendor.set(JvmVendorSpec.matching("GraalVM Community"))
            })
            debug.set(true)
            verbose.set(true)
            richOutput.set(true)

            buildArgs.add(
                "--initialize-at-build-time=" +
                    "io.ktor," +
                    "kotlin," +
                    "kotlinx," +
                    "com.fasterxml.jackson," +
                    "org.yaml.snakeyaml," +
                    "net.mamoe.yamlkt," +
                    "com.github.benmanes.caffeine," +
                    "ch.qos.logback.classic.Logger"
            )

            buildArgs.add(
                "--initialize-at-run-time=" +
                    "io.netty.buffer.AbstractByteBufAllocator," +
                    "io.netty.channel.epoll.Epoll," +
                    "io.netty.channel.epoll.EpollEventLoop," +
                    "io.netty.channel.epoll.EpollEventArray," +
                    "io.netty.channel.epoll.Native," +
                    "io.netty.channel.DefaultFileRegion," +
                    "io.netty.channel.unix.Errors"
            )
//            buildArgs.add("--initialize-at-run-time=io.netty.buffer.AbstractByteBufAllocator")
//            buildArgs.add("--initialize-at-run-time=io.netty.channel.DefaultFileRegion")
//            buildArgs.add("--initialize-at-run-time=io.netty.channel.epoll.Native")
//            buildArgs.add("--initialize-at-run-time=io.netty.channel.epoll.Epoll")
//            buildArgs.add("--initialize-at-run-time=io.netty.channel.epoll.EpollEventLoop")
//            buildArgs.add("--initialize-at-run-time=io.netty.channel.epoll.EpollEventArray")
//            buildArgs.add("--initialize-at-run-time=io.netty.channel.kqueue.KQueue")
//            buildArgs.add("--initialize-at-run-time=io.netty.channel.kqueue.KQueueEventLoop")
//            buildArgs.add("--initialize-at-run-time=io.netty.channel.kqueue.KQueueEventArray")
//            buildArgs.add("--initialize-at-run-time=io.netty.channel.kqueue.Native")
//            buildArgs.add("--initialize-at-run-time=io.netty.channel.unix.Limits")
//            buildArgs.add("--initialize-at-run-time=io.netty.channel.unix.Errors")
//            buildArgs.add("--initialize-at-run-time=io.netty.channel.unix.IovArray")

//            buildArgs.add("--trace-class-initialization=com.fasterxml.jackson.core.util.VersionUtil")


            buildArgs.add("-H:+InstallExitHandlers")
            buildArgs.add("-H:+ReportExceptionStackTraces")
            buildArgs.add("-H:-CheckToolchain")
        }
    }
}
