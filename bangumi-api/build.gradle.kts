import org.openapitools.generator.gradle.plugin.tasks.GenerateTask

plugins {
    idea
    id("org.openapi.generator") version "7.13.0"
    kotlin("jvm") version "2.1.10"
    kotlin("plugin.serialization") version "2.1.10"
}

dependencies {

    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.negotiation)
    implementation(libs.kotlinx.json)
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.1")

    testImplementation(kotlin("test"))
}

idea {
    module {
        generatedSourceDirs.add(file("src/main/gen"))
    }
}
val generatedRoot = "generated/openapi"

fun generateAction(
    specPath: String,
    pkgName: String = "moe.kurenai.bangumi",
    apiSuffix: String = "BangumiApi"
): Action<GenerateTask> {
    return Action<GenerateTask> {
        generatorName.set("kotlin")
        inputSpec.set(specPath)
        outputDir.set(layout.buildDirectory.file(generatedRoot).get().asFile.absolutePath)
        packageName.set(pkgName)
        apiNameSuffix.set(apiSuffix)
        removeOperationIdPrefix = true

        additionalProperties.set(
            mapOf(
                "apiSuffix" to apiSuffix,
                "library" to "jvm-ktor",
                "dateLibrary" to "java8",
                "serializationLibrary" to "kotlinx_serialization",
                "enumPropertyNaming" to "UPPERCASE",
                "omitGradleWrapper" to "true",
            ),
        )

        typeMappings.set(
            mapOf(
                "number" to "kotlin.Float"
            )
        )

        generateModelTests.set(true)
        generateApiTests.set(true)
        generateApiDocumentation.set(false)
        generateModelDocumentation.set(false)
    }
}

val generateApi0 = tasks.register("generateApi0", GenerateTask::class, generateAction("$projectDir/openapi.json"))
val generateApiOauth = tasks.register("generateApiOauth", GenerateTask::class, generateAction("$projectDir/oauth.yaml"))

val copyGenerateApiToSrc = tasks.register("copyGenerateApiToSrc", Copy::class) {
    dependsOn(generateApi0)
    dependsOn(generateApiOauth)
    from(layout.buildDirectory.dir("$generatedRoot/src"))
    into("${projectDir}/src")
}

tasks.register("generateApi") {
    dependsOn(copyGenerateApiToSrc)
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}
