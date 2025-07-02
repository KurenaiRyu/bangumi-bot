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
