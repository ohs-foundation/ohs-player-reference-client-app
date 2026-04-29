import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

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
        iosSimulatorArm64()
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
            implementation(projects.ohsPlayerLibrary)
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(compose.material)
            implementation(compose.materialIconsExtended)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0-rc01")
            implementation(libs.kotlinx.serialization.json)
            implementation("dev.ohs.fhir:fhir-model:1.0.0-beta03")
            implementation("dev.ohs.fhir:fhir-path:1.0.0-beta02")
        }
        val commonMain by getting {
            kotlin.srcDir(layout.buildDirectory.dir("generated/ig/commonMain/kotlin"))
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)
        }
    }
}

android {
    namespace = "dev.ohs.player.reference.app"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "dev.ohs.player.reference.app"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
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

compose.desktop {
    application {
        mainClass = "dev.ohs.player.reference.app.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "dev.ohs.player.reference.app"
            packageVersion = "1.0.0"
        }
    }
}

val generateFromIg by tasks.registering(JavaExec::class) {
    group = "build"
    description = "Generates Kotlin UI models from FHIR IG outputs"
    // External IG directory cannot be reliably tracked by Gradle incremental build
    doNotTrackState("IG source directory is external to the project")

    // We need to run the JVM executable from the codegen project
    classpath = project(":codegen").let { p ->
        val javaConvention = p.extensions.getByType(JavaPluginExtension::class.java)
        javaConvention.sourceSets.getByName("main").runtimeClasspath
    }
    mainClass.set("dev.ohs.player.reference.app.codegen.IgCodeGeneratorKt")

    // Pass the input IG directory and output directory as arguments
    //TODO Make this dynamic or read from URL for now copy the IG to the directory below, in root package
    val igDir = file("ohs-sample-ig")
    val outDir = layout.buildDirectory.dir("generated/ig/commonMain/kotlin").get().asFile

    outputs.dir(outDir)

    args(
        igDir.absolutePath,
        outDir.absolutePath
    )
}

// Make sure generation happens before common main compilation
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    if (name != "compileKotlinJvm" && !name.contains("Codegen")) {
        dependsOn(generateFromIg)
    }
}
// Make sure codegen compiles before we run the generator
generateFromIg.configure {
    dependsOn(":codegen:classes")
}
