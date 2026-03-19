import java.util.Properties
plugins {
    alias(libs.plugins.android.application)
}

// Load signing credentials from local.properties (never commit those values)
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) load(f.inputStream())
}

android {
    namespace = "com.vish_apps.tasktracker"
    compileSdk = 35

    signingConfigs {
        create("release") {
            storeFile     = file(localProps.getProperty("RELEASE_STORE_FILE", "../keystore.jks"))
            storePassword = localProps.getProperty("RELEASE_STORE_PASSWORD", "")
            keyAlias      = localProps.getProperty("RELEASE_KEY_ALIAS", "release")
            keyPassword   = localProps.getProperty("RELEASE_KEY_PASSWORD", "")
        }
    }

    defaultConfig {
        applicationId = "com.vish_apps.tasktracker"
        minSdk        = 26
        targetSdk     = 35
        versionCode   = 1
        versionName   = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled   = true
            isShrinkResources = true
            signingConfig     = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

kotlin {
    jvmToolchain(11)
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.webkit)
    implementation(libs.work.runtime.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
