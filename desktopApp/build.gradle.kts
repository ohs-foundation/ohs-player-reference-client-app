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
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
  alias(libs.plugins.kotlinMultiplatform)
  alias(libs.plugins.composeMultiplatform)
  alias(libs.plugins.composeCompiler)
  alias(libs.plugins.composeHotReload)
  id("spotless-conventions")
}

kotlin {
  jvm()

  sourceSets {
    jvmMain.dependencies {
      implementation(project(":ohs-player-reference-app"))
      implementation(compose.desktop.currentOs)
      implementation(libs.kotlinx.coroutinesSwing)
    }
    jvmTest.dependencies { implementation(compose.desktop.currentOs) }
  }
}

fun nonBlankEnv(name: String): Provider<String> =
  providers.environmentVariable(name).filter { it.isNotBlank() }

val composePackageVersion: String =
  nonBlankEnv("VERSION_NAME")
    .map { raw ->
      val numeric = raw.removePrefix("v").substringBefore('-')
      if (numeric.matches(Regex("""\d+\.\d+\.\d+"""))) {
        numeric
      } else {
        error("VERSION_NAME='$raw' is not MAJOR.MINOR.PATCH; cannot derive jpackage packageVersion")
      }
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
