plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

// Keep release credentials out of source control. An unsigned release is
// intentional for F-Droid/IzzyOnDroid source builds; local publisher builds
// use the explicit assembleSignedRelease task below.
val signingEnvironmentNames = listOf(
    "SIGNING_STORE_FILE",
    "SIGNING_STORE_PASSWORD",
    "SIGNING_KEY_ALIAS",
    "SIGNING_KEY_PASSWORD"
)
val configuredSigningEnvironmentNames = signingEnvironmentNames.filter {
    !System.getenv(it).isNullOrBlank()
}
check(
    configuredSigningEnvironmentNames.isEmpty() ||
        configuredSigningEnvironmentNames.size == signingEnvironmentNames.size
) {
    "Configure all release-signing environment variables or none: " +
        signingEnvironmentNames.joinToString()
}
val hasReleaseSigning = configuredSigningEnvironmentNames.size == signingEnvironmentNames.size

android {
    namespace = "com.uc.caffeine"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.uc.caffeine"
        minSdk = 31
        targetSdk = 36
        versionCode = 12
        versionName = "2.3.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(requireNotNull(System.getenv("SIGNING_STORE_FILE")))
                storePassword = requireNotNull(System.getenv("SIGNING_STORE_PASSWORD"))
                keyAlias = requireNotNull(System.getenv("SIGNING_KEY_ALIAS"))
                keyPassword = requireNotNull(System.getenv("SIGNING_KEY_PASSWORD"))
            }
        }
    }

    buildTypes {
        release {
            if (hasReleaseSigning) {
                signingConfig = signingConfigs["release"]
            }
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
}

tasks.register("verifyReleaseSigning") {
    group = "verification"
    description = "Fails unless all release-signing environment variables are configured."
    doLast {
        check(hasReleaseSigning) {
            "Signed builds require: ${signingEnvironmentNames.joinToString()}"
        }
    }
}

tasks.register("assembleSignedRelease") {
    group = "build"
    description = "Builds a locally signed release APK; use assembleRelease for F-Droid source builds."
    dependsOn("verifyReleaseSigning", "assembleRelease")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        optIn.addAll(
            "androidx.compose.material3.ExperimentalMaterial3ExpressiveApi"
        )
        freeCompilerArgs.add("-Xskip-prerelease-check")
        allWarningsAsErrors.set(false)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.material3.adaptive.navigation.suite)
    implementation(libs.androidx.compose.ui.text.google.fonts)
    implementation(libs.androidx.navigation.compose)

    // Room — the database
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // ViewModel + lifecycle for Compose
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // DataStore — user preferences
    implementation(libs.androidx.datastore.preferences)

    // Coil — loads images from assets/, URLs, files
    implementation(libs.coil.compose)

    // Vico Charts — beautiful charting library for Compose
    implementation(libs.vico.compose)
    implementation(libs.vico.compose.m3)

    // Add the new Navigation 3 libraries
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)

    // Health Connect
    implementation(libs.health.connect)

    // Graphics Shapes — smooth shape morphing animations
    implementation(libs.androidx.graphics.shapes)

    // Glance — Compose-style home screen widgets
    implementation(libs.glance.appwidget)
    implementation(libs.glance.material3)

    // WorkManager — reliable periodic widget refresh
    implementation(libs.androidx.work.runtime.ktx)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

}
