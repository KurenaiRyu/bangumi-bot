
plugins {
    application
    kotlin("jvm") version "2.1.10"
    kotlin("plugin.serialization") version "2.1.10"
    kotlin("plugin.lombok") version "2.1.10"
    id("io.freefair.lombok") version "5.3.0"
    base
}

allprojects {
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
}
