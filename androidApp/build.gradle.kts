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
import java.util.Properties

plugins {
  alias(libs.plugins.androidApplication)
  alias(libs.plugins.composeCompiler)
  id("spotless-conventions")
}

dependencies {
  implementation(project(":ohs-player-reference-app"))
  implementation(libs.compose.uiToolingPreview)
  implementation(libs.androidx.activity.compose)
  debugImplementation(libs.compose.uiTooling)
}

// Release signing inputs: env vars first (CI), then keystore.properties as a
// dev-time fallback. Read via the providers API so the config cache tracks them.
val keystoreProperties: Map<String, String> =
  providers
    .fileContents(rootProject.layout.projectDirectory.file("keystore.properties"))
    .asText
    .map { text ->
      val props = Properties().apply { load(text.reader()) }
      props.stringPropertyNames().associateWith(props::getProperty)
    }
    .getOrElse(emptyMap())

fun nonBlankEnv(name: String): Provider<String> =
  providers.environmentVariable(name).filter { it.isNotBlank() }

fun envOrKeystore(envName: String, fileKey: String): String? =
  nonBlankEnv(envName).orNull ?: keystoreProperties[fileKey]?.takeIf { it.isNotBlank() }

val releaseVersionName: String =
  nonBlankEnv("VERSION_NAME").map { it.removePrefix("v") }.getOrElse("0.0.0-dev")

val releaseVersionCode: Int =
  nonBlankEnv("VERSION_CODE")
    .map { raw -> raw.toIntOrNull() ?: error("VERSION_CODE='$raw' must be an integer") }
    .getOrElse(1)

val keystorePath = envOrKeystore("ANDROID_KEYSTORE_PATH", "KEYSTORE_PATH")
val keystoreAlias = envOrKeystore("ANDROID_KEY_ALIAS", "KEY_ALIAS")
val keystoreKeyPassword = envOrKeystore("ANDROID_KEY_PASSWORD", "KEY_PASSWORD")
val keystoreStorePassword = envOrKeystore("ANDROID_STORE_PASSWORD", "STORE_PASSWORD")

val hasReleaseSigning: Boolean =
  !keystorePath.isNullOrBlank() &&
    !keystoreAlias.isNullOrBlank() &&
    !keystoreKeyPassword.isNullOrBlank() &&
    !keystoreStorePassword.isNullOrBlank()

android {
  namespace = "dev.ohs.player.reference.app.android"
  compileSdk = libs.versions.android.compileSdk.get().toInt()

  defaultConfig {
    applicationId = "dev.ohs.player.reference.app"
    minSdk = libs.versions.android.minSdk.get().toInt()
    targetSdk = libs.versions.android.targetSdk.get().toInt()
    versionCode = releaseVersionCode
    versionName = releaseVersionName
  }
  packaging { resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" } }
  signingConfigs {
    if (hasReleaseSigning) {
      create("release") {
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

val isCi = providers.environmentVariable("CI").map(String::toBoolean).getOrElse(false)

if (isCi) {
  tasks
    .matching { it.name in setOf("testDebugUnitTest", "testReleaseUnitTest") }
    .configureEach { enabled = false }
}
