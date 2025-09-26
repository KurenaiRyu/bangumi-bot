import org.openapitools.generator.gradle.plugin.tasks.GenerateTask

plugins {
    idea
    alias(libs.plugins.openapi.generator)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.lombok)
    alias(libs.plugins.ksp)
    alias(libs.plugins.lombok)
}

dependencies {

    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.negotiation)
    implementation(libs.kotlinx.json)

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
        removeOperationIdPrefix.set(true)

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
                "number" to "kotlin.Float",
                "PersonCareer" to "moe.kurenai.bangumi.constant.PersonCareer",
                "WikiV0" to "moe.kurenai.bangumi.models.InfoBox",
            )
        )
        importMappings.set(
            mapOf(
                "PersonCareer" to "moe.kurenai.bangumi.constant.PersonCareer",
                "WikiV0" to "moe.kurenai.bangumi.models.InfoBox",
            )
        )

        generateModelTests.set(true)
        generateApiTests.set(true)
        generateApiDocumentation.set(false)
        generateModelDocumentation.set(false)
    }
}

val generateNormalApi = tasks.register("generateNormalApi", GenerateTask::class, generateAction("$projectDir/openapi.json"))
val generateOauthApi = tasks.register("generateOauthApi", GenerateTask::class, generateAction("$projectDir/oauth.yaml"))

val copyGenerateApiToSrc = tasks.register("copyGenerateApiToSrc", Copy::class) {
    dependsOn(generateNormalApi)
    dependsOn(generateOauthApi)
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
    compilerOptions {
        optIn.add("kotlin.time.ExperimentalTime")
    }
}
