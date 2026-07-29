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
package dev.ohs.player.reference.app.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.sun.net.httpserver.HttpServer
import dev.ohs.player.reference.app.desktopStorageDirectory
import eu.anifantakis.lib.ksafe.KSafe
import java.awt.Desktop
import java.net.InetSocketAddress
import java.net.URI
import java.security.SecureRandom
import kotlinx.coroutines.suspendCancellableCoroutine

internal actual fun createKSafe(): KSafe = KSafe(baseDir = desktopStorageDirectory)

internal actual fun secureRandomBytes(size: Int): ByteArray =
  ByteArray(size).also { SecureRandom().nextBytes(it) }

/**
 * Desktop login: open the system browser to the identity provider and capture the redirect on a
 * short-lived localhost loopback server (the OAuth 2.0 native-app recommendation, RFC 8252).
 */
actual class AuthorizationLauncher(private val port: Int) : AuthorizationLauncherApi {

  actual override val redirectUri: String = "http://127.0.0.1:$port/callback"

  actual override suspend fun authorize(authUrl: String): AuthResult =
    suspendCancellableCoroutine { continuation ->
      val server = HttpServer.create(InetSocketAddress("127.0.0.1", port), 0)
      server.createContext("/callback") { exchange ->
        val callbackUrl = "$redirectUri?${exchange.requestURI.rawQuery.orEmpty()}"
        val html =
          "<!doctype html><html><body style=\"font-family:sans-serif;padding:2rem\">" +
            "<h3>You can return to the app</h3><p>You may close this window.</p>" +
            "</body></html>"
        val bytes = html.encodeToByteArray()
        exchange.responseHeaders.add("Content-Type", "text/html; charset=utf-8")
        exchange.sendResponseHeaders(200, bytes.size.toLong())
        exchange.responseBody.use { it.write(bytes) }
        Thread { server.stop(0) }.start()
        if (continuation.isActive)
          continuation.resumeWith(Result.success(AuthResult.Success(callbackUrl)))
      }
      server.start()
      continuation.invokeOnCancellation { server.stop(0) }

      runCatching {
          val desktop = Desktop.getDesktop()
          if (Desktop.isDesktopSupported() && desktop.isSupported(Desktop.Action.BROWSE)) {
            desktop.browse(URI(authUrl))
          } else {
            error("Opening a browser is not supported on this desktop environment")
          }
        }
        .onFailure {
          server.stop(0)
          if (continuation.isActive) {
            continuation.resumeWith(
              Result.success(AuthResult.Failure(it.message ?: "Failed to open browser"))
            )
          }
        }
    }

  actual fun consumeRedirectCallback(): String? = null
}

@Composable
actual fun rememberAuthorizationLauncher(): AuthorizationLauncher = remember {
  AuthorizationLauncher(GeneratedAuthConfig.DESKTOP_REDIRECT_PORT)
}
