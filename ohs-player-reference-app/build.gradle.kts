import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "OhsPlayerReferenceApp"
            isStatic = true
        }
    }

    jvm()

    js {
        browser()
        binaries.executable()
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        binaries.executable()
    }

    sourceSets {
        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.activity.compose)
        }
        commonMain.dependencies {
            implementation(project(":ohs-player-library"))
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material)
            implementation(libs.compose.material3)
            implementation(libs.compose.materialIconsCore)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.navigation.compose)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.compose.uiTest)
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)
        }
        jvmTest.dependencies {
            implementation(compose.desktop.currentOs)
        }
    }
}

// Env vars are the primary input (CI). keystore.properties is a dev-time
// fallback so contributors can test release signing without exporting vars in
// their shell. See keystore.properties.template for the expected keys; the
// real file is gitignored — never commit secrets. Reads use the providers API
// so the configuration cache tracks them as declared inputs.
val keystoreProperties: Map<String, String> = providers
    .fileContents(rootProject.layout.projectDirectory.file("keystore.properties"))
    .asText
    .map { text ->
        val props = Properties().apply { load(text.reader()) }
        props.stringPropertyNames().associateWith(props::getProperty)
    }
    .getOrElse(emptyMap())

fun envOrFile(envName: String, fileKey: String): String? = providers.environmentVariable(envName).orNull?.takeIf { it.isNotBlank() }
    ?: keystoreProperties[fileKey]?.takeIf { it.isNotBlank() }

// "0.0.0-dev" makes any accidentally-shipped dev build obviously distinct from
// a real release; a missing VERSION_NAME on CI is a misconfiguration, not a
// silent fallback to "1.0".
val releaseVersionName: String = providers.environmentVariable("VERSION_NAME")
    .map { it.removePrefix("v") }
    .getOrElse("0.0.0-dev")

val releaseVersionCode: Int = providers.environmentVariable("VERSION_CODE")
    .map { it.toInt() }
    .getOrElse(1)

val keystorePath = envOrFile("ANDROID_KEYSTORE_PATH", "KEYSTORE_PATH")
val keystoreAlias = envOrFile("ANDROID_KEY_ALIAS", "KEY_ALIAS")
val keystoreKeyPassword = envOrFile("ANDROID_KEY_PASSWORD", "KEY_PASSWORD")
val keystoreStorePassword = envOrFile("ANDROID_STORE_PASSWORD", "STORE_PASSWORD")

val hasReleaseSigning: Boolean =
    !keystorePath.isNullOrBlank() &&
        !keystoreAlias.isNullOrBlank() &&
        !keystoreKeyPassword.isNullOrBlank() &&
        !keystoreStorePassword.isNullOrBlank()

android {
    namespace = "dev.ohs.player.reference.app"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "dev.ohs.player.reference.app"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = releaseVersionCode
        versionName = releaseVersionName
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                // Disable the legacy JAR signing scheme (V1); rely on APK Signature
                // Scheme V2+ which Android 7.0+ requires and which all current
                // distribution channels expect.
                enableV1Signing = false
                enableV2Signing = true
                storeFile = file(keystorePath!!)
                keyAlias = keystoreAlias
                keyPassword = keystoreKeyPassword
                storePassword = keystoreStorePassword
            }
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    debugImplementation(libs.compose.uiTooling)
}

// WiX/MSI requires a strict MAJOR.MINOR.PATCH numeric version, so strip any
// Semantic Version pre-release suffix (e.g. -alpha.1) from VERSION_NAME for the desktop
// installers. Android's versionName keeps the suffix; this drift is intentional.
// Uses the providers API so the configuration cache tracks VERSION_NAME as a
// declared input.
val composePackageVersion: String = providers.environmentVariable("VERSION_NAME")
    .map { raw ->
        val numeric = raw.removePrefix("v").substringBefore('-')
        if (numeric.matches(Regex("""\d+\.\d+\.\d+"""))) numeric else "1.0.0"
    }
    .getOrElse("1.0.0")

compose.desktop {
    application {
        mainClass = "dev.ohs.player.reference.app.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb, TargetFormat.Rpm)
            packageName = "dev.ohs.player.reference.app"
            packageVersion = composePackageVersion
        }
    }
}
