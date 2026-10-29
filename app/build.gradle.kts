import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element
import org.w3c.dom.Node

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

// Weblate commits an empty values-<locale>/strings.xml stub as soon as someone clicks
// "Start new translation". Require at least one translated resource before packaging a locale.
val minTranslatedStrings = 1
val bcp47QualifierRegex = Regex("^b\\+([A-Za-z]{2,3}(?:\\+[A-Za-z0-9]+)*)$")
val legacyLocaleQualifierRegex = Regex("^([a-z]{2,3})(?:-r([A-Z]{2}|[0-9]{3}))?$")

fun localeResourceQualifier(dirName: String): String? {
    if (!dirName.startsWith("values-")) return null
    val qualifier = dirName.removePrefix("values-")
    // "values-car" is Android Auto's UI mode qualifier in AAPT2, not a locale directory.
    if (qualifier == "car") return null

    if (bcp47QualifierRegex.matches(qualifier) || legacyLocaleQualifierRegex.matches(qualifier)) {
        // localeFilters passes qualifiers directly to AAPT2's -c flag, which expects Android
        // resource qualifier syntax (e.g. "zh-rCN" or "b+sr+Latn") rather than BCP-47 "zh-CN".
        return qualifier
    }
    return null
}

fun translatedResourceNames(localeResDir: java.io.File): Set<String> {
    val xmlFiles = localeResDir
        .listFiles { file -> file.isFile && file.extension.equals("xml", ignoreCase = true) }
        .orEmpty()
    if (xmlFiles.isEmpty()) return emptySet()

    val factory = DocumentBuilderFactory.newInstance()
    val names = sortedSetOf<String>()
    for (xmlFile in xmlFiles) {
        val doc = factory.newDocumentBuilder().parse(xmlFile)
        val root = doc.documentElement ?: continue
        val children = root.childNodes
        for (i in 0 until children.length) {
            val node = children.item(i)
            if (node.nodeType != Node.ELEMENT_NODE) continue
            val element = node as Element
            if (element.getAttribute("translatable") == "false") continue
            val translated = when (element.tagName) {
                "string" -> element.textContent.trim().isNotEmpty()
                "plurals", "string-array" -> {
                    val items = element.getElementsByTagName("item")
                    (0 until items.length).any { idx -> items.item(idx).textContent.trim().isNotEmpty() }
                }
                else -> false
            }
            if (translated) names += "${element.tagName}/${element.getAttribute("name")}"
        }
    }
    return names
}

fun countTranslatedResourceEntries(localeResDir: java.io.File): Int =
    translatedResourceNames(localeResDir).size

val mainResDir = layout.projectDirectory.dir("src/main/res").asFile
val nonBlankTranslatedLocales: List<String> = mainResDir
    .listFiles { dir -> dir.isDirectory && dir.name.startsWith("values-") }
    .orEmpty()
    .mapNotNull { dir ->
        val qualifier = localeResourceQualifier(dir.name) ?: return@mapNotNull null
        qualifier.takeIf { countTranslatedResourceEntries(dir) >= minTranslatedStrings }
    }
    .distinct()
    .sorted()

// Per-locale completion (% of the English strings that have a translation), shown on the
// Language screen like Weblate's progress. Computed from the checked-in resources, so the
// build stays reproducible. Format: "de=93;zh-CN=80".
val baseResourceNames = translatedResourceNames(mainResDir.resolve("values"))
val translationCompletion: String = nonBlankTranslatedLocales.joinToString(";") { qualifier ->
    val tag = if (qualifier.startsWith("b+")) {
        qualifier.removePrefix("b+").replace('+', '-')
    } else {
        qualifier.replace("-r", "-")
    }
    val translated = translatedResourceNames(mainResDir.resolve("values-$qualifier"))
        .count { it in baseResourceNames }
    val percent = if (baseResourceNames.isEmpty()) 100 else translated * 100 / baseResourceNames.size
    "$tag=$percent"
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
        versionCode = 17
        versionName = "2.5.1"

        buildConfigField("String", "TRANSLATION_COMPLETION", "\"$translationCompletion\"")

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
    androidResources {
        // Derives res/xml/_generated_res_locale_config.xml (wired into the manifest's
        // android:localeConfig automatically) from the values-<locale> dirs that exist
        // at build time, while localeFilters blocks blank Weblate stubs (0 translated
        // strings) from leaking into the supported-locale list or packaged APK.
        generateLocaleConfig = true
        localeFilters += listOf("en") + nonBlankTranslatedLocales
    }
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
}

tasks.register("verifyTranslations") {
    group = "verification"
    description = "Fails if any values-<locale> resource directory contains zero translated strings."
    doLast {
        val blankLocaleDirs = mainResDir
            .listFiles { dir -> dir.isDirectory && dir.name.startsWith("values-") }
            .orEmpty()
            .filter { dir -> localeResourceQualifier(dir.name) != null }
            .filter { dir -> countTranslatedResourceEntries(dir) < minTranslatedStrings }
            .map { it.name }
            .sorted()
        check(blankLocaleDirs.isEmpty()) {
            "Blank translation directories are not allowed (need >= $minTranslatedStrings translated string): " +
                blankLocaleDirs.joinToString()
        }
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
    // Release Kotlin always compiles from scratch. An incremental release compile reused a stale
    // Compose stability verdict for UserSettings in AutomationSettings.kt (`changed` instead of
    // `changedInstance`), so the published v2.4.0 APK didn't match the stores' clean rebuild (#50).
    if (name.contains("Release")) {
        incremental = false
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

    // Haze — backdrop blur behind the floating toolbar
    implementation(libs.haze)

    // Barcode scanning — CameraX viewfinder + ZXing decoding
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.compose)
    implementation(libs.zxing.core)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

}
