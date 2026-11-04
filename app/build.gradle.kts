import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element
import org.w3c.dom.Node
import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.variant.BuildConfigField

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

val resourceRoots = listOf("main", "foss", "play").map { layout.projectDirectory.dir("src/$it/res").asFile }
val mainResDir = resourceRoots.first()
fun translatedLocales(roots: List<java.io.File>): List<String> = roots
    .flatMap { root -> root.listFiles { dir -> dir.isDirectory && dir.name.startsWith("values-") }.orEmpty().toList() }
    .mapNotNull { dir ->
        val qualifier = localeResourceQualifier(dir.name) ?: return@mapNotNull null
        qualifier.takeIf { countTranslatedResourceEntries(dir) >= minTranslatedStrings }
    }.distinct().sorted()
val nonBlankTranslatedLocales = translatedLocales(resourceRoots)

// Per-locale completion (% of the English strings that have a translation), shown on the
// Language screen like Weblate's progress. Computed from the checked-in resources, so the
// build stays reproducible. Format: "de=93;zh-CN=80".
fun translationCompletion(flavor: String): String {
    val roots = listOf(mainResDir, layout.projectDirectory.dir("src/$flavor/res").asFile)
    val baseResourceNames = roots.flatMap { translatedResourceNames(it.resolve("values")) }.toSet()
    return translatedLocales(roots).joinToString(";") { qualifier ->
        val tag = if (qualifier.startsWith("b+")) {
            qualifier.removePrefix("b+").replace('+', '-')
        } else {
            qualifier.replace("-r", "-")
        }
        val translated = roots.flatMap { translatedResourceNames(it.resolve("values-$qualifier")) }.toSet()
            .count { it in baseResourceNames }
        val percent = if (baseResourceNames.isEmpty()) 100 else translated * 100 / baseResourceNames.size
        "$tag=$percent"
    }
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
    bundle {
        // The in-app picker must also switch languages on Android 12 without Play Core.
        language { enableSplit = false }
    }
    lint {
        // Weblate locales intentionally ship partial translations with English fallback.
        // Keep every missing entry in reports; it is translation work, not a build blocker.
        warning.add("MissingTranslation")
    }

    namespace = "com.uc.caffeine"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.uc.caffeine"
        minSdk = 31
        targetSdk = 36
        versionCode = 17
        versionName = "2.5.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    flavorDimensions += "distribution"
    productFlavors {
        listOf("foss", "play").forEach { edition ->
            create(edition) {
                dimension = "distribution"
                buildConfigField("String", "DISTRIBUTION", "\"$edition\"")
                buildConfigField("String", "TRANSLATION_COMPLETION", "\"${translationCompletion(edition)}\"")
                manifestPlaceholders["distribution"] = edition
                if (edition == "play") proguardFile("src/play/revenuecat-rules.pro")
            }
        }
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
        val blankLocaleDirs = resourceRoots.flatMap { root ->
            root.listFiles { dir -> dir.isDirectory && dir.name.startsWith("values-") }.orEmpty().toList()
        }
            .filter { dir -> localeResourceQualifier(dir.name) != null }
            .filter { dir -> countTranslatedResourceEntries(dir) < minTranslatedStrings }
            .map { it.relativeTo(projectDir).path }
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
    description = "Compatibility alias for the publisher-signed FOSS release asset."
    dependsOn("assembleSignedFossRelease")
}

tasks.register("assembleSignedFossRelease") {
    group = "build"
    dependsOn("verifyReleaseSigning", "stageFossRelease")
}

tasks.register("bundleSignedPlayRelease") {
    group = "build"
    dependsOn("verifyReleaseSigning", "bundlePlayRelease")
}

androidComponents {
    onVariants(selector().withFlavor("distribution" to "play")) { variant ->
        val debug = variant.buildType == "debug"
        val key = if (debug) providers.gradleProperty("RC_DEBUG_PUBLIC_KEY")
            .orElse(providers.environmentVariable("RC_DEBUG_PUBLIC_KEY")).getOrElse("")
        else providers.gradleProperty("RC_ANDROID_PUBLIC_KEY")
            .orElse(providers.environmentVariable("RC_ANDROID_PUBLIC_KEY")).getOrElse("")
        require(key.isEmpty() || key.matches(Regex(if (debug) "(goog_|test_)[A-Za-z0-9_]+" else "goog_[A-Za-z0-9_]+"))) {
            "Use a public Android SDK key; Test Store keys are debug-only."
        }
        requireNotNull(variant.buildConfigFields).put("RC_PUBLIC_KEY", BuildConfigField("String", "\"$key\"", null))
        // Where Plus is sold until BillDesk PA-CB KYC allows cross-border sales. Comma-separated ISO
        // country codes or "*". RevenueCat offering metadata "plus_sale_countries" overrides it remotely.
        val saleCountries = providers.gradleProperty("PLUS_SALE_COUNTRIES").getOrElse("IN")
        require(saleCountries.matches(Regex("""\*|[A-Za-z]{2}(\s*,\s*[A-Za-z]{2})*"""))) {
            "PLUS_SALE_COUNTRIES must be \"*\" or comma-separated two-letter country codes."
        }
        requireNotNull(variant.buildConfigFields).put("PLUS_SALE_COUNTRIES", BuildConfigField("String", "\"$saleCountries\"", null))
        // Release activation requires a reviewed source change after owner approval and billing validation.
        listOf("PLUS_PURCHASES_ENABLED", "PLUS_POLICY_ENABLED").forEach { name ->
            val enabled = debug && providers.gradleProperty(name).getOrElse("false").toBoolean()
            requireNotNull(variant.buildConfigFields).put(name, BuildConfigField("boolean", enabled.toString(), null))
        }
    }
    onVariants(selector().withFlavor("distribution" to "foss").withBuildType("release")) { variant ->
        val apkDirectory = variant.artifacts.get(SingleArtifact.APK)
        val manifest = variant.artifacts.get(SingleArtifact.MERGED_MANIFEST)
        val aapt2 = sdkComponents.sdkDirectory.map { sdk ->
            sdk.file("build-tools/${android.buildToolsVersion}/aapt2" +
                if (System.getProperty("os.name").startsWith("Windows")) ".exe" else "")
        }
        val graph = layout.buildDirectory.file("reports/foss-isolation/runtime-dependencies.txt")
        val dependencyReport = tasks.register("writeFossReleaseDependencies") {
            outputs.file(graph)
            // Resolve every run: stale reports must never authorize a changed dependency graph.
            outputs.upToDateWhen { false }
            doLast {
                val reports = listOf("fossDebug", "fossRelease").flatMap { edition ->
                    listOf("CompileClasspath", "RuntimeClasspath").map { suffix ->
                        val configurationName = edition + suffix
                        val coordinates = configurations.getByName(configurationName)
                            .incoming.resolutionResult.allComponents.mapNotNull { it.moduleVersion?.toString() }.sorted()
                        "# $configurationName\n" + coordinates.joinToString("\n")
                    }
                }
                graph.get().asFile.apply { parentFile.mkdirs(); writeText(reports.joinToString("\n") + "\n") }
            }
        }
        val isolation = tasks.register<Exec>("verifyFossReleaseIsolation") {
            group = "verification"
            description = "Rejects billing, account, analytics and sync vendors in FOSS inputs and APK."
            dependsOn(dependencyReport, "assembleFossRelease")
            inputs.dir(apkDirectory)
            inputs.file(manifest)
            inputs.file(graph)
            inputs.file(rootProject.file("tools/verify_foss_isolation.py"))
            commandLine(providers.gradleProperty("pythonExecutable").getOrElse("python3"),
                rootProject.file("tools/verify_foss_isolation.py"),
                "--dependencies", graph.get().asFile,
                "--manifest", manifest.get().asFile,
                "--apk-dir", apkDirectory.get().asFile,
                "--aapt2", aapt2.get().asFile)
        }
        tasks.register<Copy>("stageFossRelease") {
            group = "build"
            description = "Stages only the FOSS APK as the established GitHub/Izzy app-release.apk asset."
            dependsOn(isolation)
            from(apkDirectory) { include("*.apk"); rename { "app-release.apk" } }
            into(layout.buildDirectory.dir("outputs/foss-release"))
        }
    }
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
    "playImplementation"(libs.revenuecat.purchases) {
        // The SDK supports absence of this optional helper; recovery is an explicit store restore.
        exclude(group = "com.google.android.gms", module = "play-services-auth-blockstore")
    }
    // Google Drive backup (Play only). The FOSS guard rejects every com.google.android.gms artifact.
    "playImplementation"(libs.play.services.auth)
    constraints {
        add("playImplementation", "androidx.fragment:fragment:1.3.6") {
            because("Billing's Play services pull Fragment 1.1.0; Activity Result APIs require at least 1.3.0.")
        }
    }
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
    implementation(libs.androidx.adaptive.navigation3)

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
    // JVM tests only: Android's org.json is a stub off-device. Never packaged.
    testImplementation("org.json:json:20240303")
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

}
