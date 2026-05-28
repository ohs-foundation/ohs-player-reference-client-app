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
import com.diffplug.gradle.spotless.SpotlessExtension

plugins {
  // this is necessary to avoid the plugins to be loaded multiple times
  // in each subproject's classloader
  alias(libs.plugins.androidApplication) apply false
  alias(libs.plugins.androidLibrary) apply false
  alias(libs.plugins.androidKotlinMultiplatformLibrary) apply false
  alias(libs.plugins.composeCompiler) apply false
  alias(libs.plugins.composeHotReload) apply false
  alias(libs.plugins.composeMultiplatform) apply false
  alias(libs.plugins.kotlinMultiplatform) apply false
  alias(libs.plugins.spotless)
}

// Spotless's `ratchetFrom` resolves at configuration time, so any Gradle
// invocation on a checkout without `origin/main` (shallow CI jobs, tag-push
// release builds) would fail to configure. Detect once and opt in only when
// the ref is available; the dedicated `spotless` CI job fetches it.
// Uses `providers.exec` so the result is captured by the configuration cache.
val hasOriginMain: Boolean =
  providers
    .exec {
      commandLine("git", "rev-parse", "--verify", "--quiet", "origin/main")
      isIgnoreExitValue = true
    }
    .result
    .map { it.exitValue == 0 }
    .getOrElse(false)

allprojects {
  apply(plugin = rootProject.libs.plugins.spotless.get().pluginId)

  configure<SpotlessExtension> {
    val ktfmtVersion = rootProject.libs.versions.ktfmt.get()
    val licenseHeaderFile = rootProject.file("license-header.txt")

    // Don't hook `spotlessCheck` into the lifecycle `check` task. The
    // dedicated `spotless` CI job runs `spotlessCheck` directly, so every
    // other job (and local `./gradlew check`) stays free of formatter work.
    isEnforceCheck = false

    if (hasOriginMain) {
      ratchetFrom = "origin/main"
    }

    kotlin {
      target("src/**/*.kt")
      ktfmt(ktfmtVersion).googleStyle()
      licenseHeaderFile(licenseHeaderFile)
    }
    kotlinGradle {
      target("*.gradle.kts")
      ktfmt(ktfmtVersion).googleStyle()
      licenseHeaderFile(licenseHeaderFile, "(^(?![\\/ ]\\*).*$)")
    }
  }
}
