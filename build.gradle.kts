import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
//    id("io.quarkus")
    kotlin("jvm") version "1.7.20"
    application
}

group = "moe.kurenai.bot"
version = "1.0-SNAPSHOT"

repositories {
    mavenLocal()
    maven { url = uri("https://maven.aliyun.com/repository/public/") }
    maven { url = uri("https://maven.aliyun.com/repository/spring/") }
    mavenCentral()
}

fun gpr(url: String): (MavenArtifactRepository).() -> Unit {
    return {
        this.url = uri(url)
        credentials {
            username = project.findProperty("gpr.user") as String? ?: System.getenv("USERNAME")
            password = project.findProperty("gpr.key") as String? ?: System.getenv("TOKEN")
        }
    }
}

val vertxVersion = "4.2.3"
val log4j = "2.19.0"
dependencies {
    implementation("moe.kurenai.bgm", "bangumi-sdk", "0.0.1-SNAPSHOT")
    implementation("moe.kurenai.tdlight", "td-light-sdk", "0.1.0-SNAPSHOT")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:1.6.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-slf4j:1.6.4")

    implementation("org.redisson:redisson:3.17.7")

    implementation("org.jsoup:jsoup:1.15.3")

    implementation("org.reflections", "reflections", "0.10.2")

    api("io.ktor:ktor-client-core:2.1.0")


    //logging
    implementation("org.apache.logging.log4j:log4j-core:$log4j")
    implementation("org.apache.logging.log4j:log4j-slf4j-impl:$log4j")
    implementation("com.lmax:disruptor:3.4.4")

    implementation(platform("io.vertx:vertx-stack-depchain:$vertxVersion"))
    implementation("io.vertx:vertx-web")
    implementation("io.vertx:vertx-lang-kotlin")

    implementation("com.fasterxml.jackson.dataformat", "jackson-dataformat-yaml", "2.13.0")

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

tasks.jar {
    dependsOn("clearLib")
    dependsOn("copyLib")
    exclude("**/*.jar")
    manifest {
        attributes["Manifest-Version"] = "1.0"
        attributes["Multi-Release"] = "true"
        attributes["Main-Class"] = main
        attributes["Class-Path"] = configurations.runtimeClasspath.get().files.map { "lib/${it.name}" }.joinToString(" ")
    }
    archiveFileName.set("${rootProject.name}.jar")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf(
            "-Xjsr305=strict",
        )
        jvmTarget = JavaVersion.VERSION_17.toString()
        javaParameters = true
    }
}