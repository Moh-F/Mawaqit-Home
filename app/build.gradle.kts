plugins {
    id("com.android.application")
}

android {
    namespace = "com.moh.mawaqithome"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.moh.mawaqithome"
        minSdk = 22          // Fire OS 6 = Android 7.1 (API 25)
        targetSdk = 28
        // CI run number drives the version, so every build is a higher version than the last
        val run = (System.getenv("GITHUB_RUN_NUMBER") ?: "0").toInt()
        versionCode = 100 + run
        versionName = "2.1.$run"
    }

    // One fixed key for every build, so new APKs install OVER the old one and keep all settings.
    // The key is NOT in the repository: CI decodes it from GitHub Secrets into these variables.
    val ksFile: String? = System.getenv("KEYSTORE_FILE")
    val ksPass: String? = System.getenv("KEYSTORE_PASSWORD")
    signingConfigs {
        if (ksFile != null && ksPass != null) {
            create("mawaqit") {
                storeFile = file(ksFile)
                storePassword = ksPass
                keyAlias = "mawaqit"
                keyPassword = ksPass
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("mawaqit")
        }
        debug {
            signingConfig = signingConfigs.findByName("mawaqit")
        }
    }

    lint {
        // targetSdk 28 on purpose (Fire OS 6 / sideloaded app); the Play-Store rule does not apply
        checkReleaseBuilds = false
        abortOnError = false
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
    // Talks to the device's own adb daemon (open hidden settings screens, sleep the device)
    implementation("dev.mobile:dadb:1.2.10")
}
