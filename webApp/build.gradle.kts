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

plugins {
  alias(libs.plugins.kotlinMultiplatform)
  alias(libs.plugins.composeMultiplatform)
  alias(libs.plugins.composeCompiler)
  id("spotless-conventions")
}

kotlin {
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
    val webMain = create("webMain") { dependsOn(commonMain.get()) }
    named("jsMain") { dependsOn(webMain) }
    named("wasmJsMain") { dependsOn(webMain) }

    webMain.dependencies {
      implementation(project(":ohs-player-reference-app"))
      implementation(libs.compose.ui)
    }
  }
}

val isCi = providers.environmentVariable("CI").map(String::toBoolean).getOrElse(false)

if (isCi) {
  tasks
    .matching {
      it.name in
        setOf(
          "compileTestDevelopmentExecutableKotlinJs",
          "compileTestProductionExecutableKotlinJs",
          "compileProductionExecutableKotlinJs",
          "jsBrowserTest",
          "wasmJsBrowserTest",
        )
    }
    .configureEach { enabled = false }
}
