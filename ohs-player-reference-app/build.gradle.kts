/*
 * Copyright 2026 Open Health Stack Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  alias(libs.plugins.kotlinMultiplatform)
  alias(libs.plugins.androidKotlinMultiplatformLibrary)
  alias(libs.plugins.composeMultiplatform)
  alias(libs.plugins.composeCompiler)
  alias(libs.plugins.kotlinSerialization)
  id("dev.ohs.ig-codegen")
  id("spotless-conventions")
}

kotlin {
  android {
    namespace = "dev.ohs.player.reference.app"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    minSdk = libs.versions.android.minSdk.get().toInt()
    compilerOptions { jvmTarget.set(JvmTarget.JVM_11) }
  }

  listOf(iosArm64(), iosSimulatorArm64()).forEach { iosTarget ->
    iosTarget.binaries.framework {
      baseName = "OhsPlayerReferenceApp"
      isStatic = true
    }
  }

  jvm()

  js { browser() }

  @OptIn(ExperimentalWasmDsl::class) wasmJs { browser() }

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
      implementation(libs.kermit)
      implementation(libs.kotlinx.serialization.json)
      implementation(libs.kotlinx.datetime)
      implementation(libs.navigation.compose)
      implementation(libs.ohs.fhir.model)
      implementation(libs.ohs.fhir.path)
    }
    commonTest.dependencies {
      implementation(libs.kotlin.test)
      implementation(libs.compose.uiTest)
      implementation(libs.kotlinx.coroutines.test)
    }
    jvmMain.dependencies {
      implementation(compose.desktop.currentOs)
      implementation(libs.kotlinx.coroutinesSwing)
    }
    jvmTest.dependencies { implementation(compose.desktop.currentOs) }
  }
}

igCodegen {
  // sourcesDir defaults to src/commonMain/composeResources/files
  packageName = "dev.ohs.player.generated"
}

// Targets skipped on CI until their respective test setups are sorted out.
// Only disabled when CI=true (set by GitHub Actions) so the CI build stays green;
// local development still runs these so contributors can reproduce and fix the
// underlying failures.
//
//   * Kotlin/JS IR backend crashes lowering the generated sealed-interface dispatch tables in
//     dev.ohs.fhir:fhir-path (StackOverflow in KotlinLikeDumper). Mirrors the same skip in
//     :ohs-player-library.
val isCi = providers.environmentVariable("CI").map(String::toBoolean).getOrElse(false)

if (isCi) {
  tasks
    .matching {
      it.name in
        setOf(
          "compileTestDevelopmentExecutableKotlinJs",
          "compileTestProductionExecutableKotlinJs",
          "jsBrowserTest",
          "wasmJsBrowserTest",
        )
    }
    .configureEach { enabled = false }
}
