import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKotlinMultiplatformLibrary)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.spotless)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

spotless {
    kotlin {
        target("**/*.kt")
        ktlint()
    }
    kotlinGradle {
        target("**/*.kts")
        ktlint()
    }
}

kotlin {
    // TODO(AGP-9.0): rename `androidLibrary { }` to `android { }` once AGP is upgraded.
    androidLibrary {
        namespace = "dev.ohs.player.library"
        compileSdk =
            libs.versions.android.compileSdk
                .get()
                .toInt()
        minSdk =
            libs.versions.android.minSdk
                .get()
                .toInt()

        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }

        withHostTest {}
    }

    iosArm64()
    iosSimulatorArm64()

    jvm()

    js {
        browser()
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }

    sourceSets {
        commonMain.dependencies {
            api(libs.kotlinx.serialization.json)
            implementation(libs.ohs.fhir.model)
            api(libs.ohs.fhir.path)
            implementation(libs.kotlinx.datetime)
            implementation(libs.compose.runtime)
            implementation(libs.compose.ui)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.compose.uiTest)
            implementation(libs.kotlinx.coroutines.test)
        }
        jvmTest.dependencies {
            implementation(compose.desktop.currentOs)
        }
    }
}
