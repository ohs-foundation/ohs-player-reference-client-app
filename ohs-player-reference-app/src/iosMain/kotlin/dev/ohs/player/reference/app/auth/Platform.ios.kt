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
@file:OptIn(ExperimentalForeignApi::class)

package dev.ohs.player.reference.app.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import eu.anifantakis.lib.ksafe.KSafe
import eu.anifantakis.lib.ksafe.KSafeConfig
import kotlin.coroutines.resume
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.AuthenticationServices.ASPresentationAnchor
import platform.AuthenticationServices.ASWebAuthenticationPresentationContextProvidingProtocol
import platform.AuthenticationServices.ASWebAuthenticationSession
import platform.Foundation.NSError
import platform.Foundation.NSURL
import platform.Security.SecRandomCopyBytes
import platform.Security.errSecSuccess
import platform.Security.kSecRandomDefault
import platform.UIKit.UIApplication
import platform.UIKit.UISceneActivationStateForegroundActive
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowScene
import platform.darwin.NSObject

/**
 * `requireUnlockedDevice = false` keeps the Keychain key at
 * `kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly`, so the background `BGProcessingTask` sync can
 * read the session while the device is locked (once it has been unlocked at least once since boot).
 * Setting it `true` would break headless sync on a locked device.
 */
internal actual fun createKSafe(): KSafe =
  KSafe(config = KSafeConfig(requireUnlockedDevice = false))

internal actual fun secureRandomBytes(size: Int): ByteArray {
  val bytes = ByteArray(size)
  val status =
    bytes.usePinned { pinned ->
      SecRandomCopyBytes(kSecRandomDefault, size.toULong(), pinned.addressOf(0))
    }
  check(status == errSecSuccess) { "SecRandomCopyBytes failed (OSStatus $status)" }
  return bytes
}

/**
 * iOS login via ASWebAuthenticationSession — the platform-blessed flow that shares cookies with
 * Safari and returns the callback URL directly to the app (no AppDelegate plumbing needed).
 */
actual class AuthorizationLauncher(
  actual override val redirectUri: String,
  private val callbackScheme: String,
) : AuthorizationLauncherApi {
  private val contextProvider = PresentationContextProvider()

  actual override suspend fun authorize(authUrl: String): AuthResult =
    suspendCancellableCoroutine { continuation ->
      val session =
        ASWebAuthenticationSession(
          uRL = NSURL(string = authUrl),
          callbackURLScheme = callbackScheme,
        ) { callbackURL: NSURL?, error: NSError? ->
          val result =
            when {
              callbackURL != null -> AuthResult.Success(callbackURL.absoluteString ?: "")
              // ASWebAuthenticationSessionErrorCodeCanceledLogin == 1
              error != null && error.code.toInt() == 1 -> AuthResult.Canceled
              error != null -> AuthResult.Failure(error.localizedDescription)
              else -> AuthResult.Failure("Unknown authentication error")
            }
          if (continuation.isActive) continuation.resume(result)
        }
      session.presentationContextProvider = contextProvider
      session.prefersEphemeralWebBrowserSession = false
      continuation.invokeOnCancellation { session.cancel() }
      session.start()
    }

  actual override fun consumeRedirectCallback(): String? = null
}

private class PresentationContextProvider :
  NSObject(), ASWebAuthenticationPresentationContextProvidingProtocol {
  @Suppress("DEPRECATION")
  override fun presentationAnchorForWebAuthenticationSession(
    session: ASWebAuthenticationSession
  ): ASPresentationAnchor {
    val app = UIApplication.sharedApplication
    val scenes = app.connectedScenes.filterIsInstance<UIWindowScene>()
    val scene =
      scenes.firstOrNull { it.activationState == UISceneActivationStateForegroundActive }
        ?: scenes.firstOrNull()
    val window = scene?.windows?.filterIsInstance<UIWindow>()?.firstOrNull()
    return window ?: app.keyWindow ?: UIWindow()
  }
}

@Composable
actual fun rememberAuthorizationLauncher(): AuthorizationLauncher = remember {
  AuthorizationLauncher(
    redirectUri = "${GeneratedAuthConfig.REDIRECT_SCHEME}://${GeneratedAuthConfig.REDIRECT_HOST}",
    callbackScheme = GeneratedAuthConfig.REDIRECT_SCHEME,
  )
}
