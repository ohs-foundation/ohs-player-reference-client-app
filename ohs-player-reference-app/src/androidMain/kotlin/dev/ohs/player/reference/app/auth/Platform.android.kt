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

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import dev.ohs.player.reference.app.MainActivity
import eu.anifantakis.lib.ksafe.KSafe
import java.security.SecureRandom
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

/** Holds the Application context so KSafe (and others) can be created from common code. */
internal object AndroidAppContext {
  lateinit var application: Application
    private set

  fun init(app: Application) {
    application = app
  }
}

internal actual fun createKSafe(): KSafe = KSafe(AndroidAppContext.application)

internal actual fun secureRandomBytes(size: Int): ByteArray =
  ByteArray(size).also { SecureRandom().nextBytes(it) }

/**
 * Bridges the deep-link redirect (a separate Activity) back to the awaiting coroutine. A null
 * completion means the user dismissed the browser without finishing.
 */
internal object AuthRedirectBus {
  @Volatile var pending: CompletableDeferred<String?>? = null
}

actual class AuthorizationLauncher(private val activity: Activity, actual val redirectUri: String) {
  actual suspend fun authorize(authUrl: String): AuthResult {
    val deferred = CompletableDeferred<String?>()
    AuthRedirectBus.pending = deferred
    val lifecycle = (activity as? LifecycleOwner)?.lifecycle
    val cancelOnReturn =
      object : DefaultLifecycleObserver {
        private var leftForBrowser = false

        override fun onStop(owner: LifecycleOwner) {
          leftForBrowser = true
        }

        override fun onResume(owner: LifecycleOwner) {
          if (leftForBrowser && !deferred.isCompleted) deferred.complete(null)
        }
      }
    return try {
      lifecycle?.let { withContext(Dispatchers.Main) { it.addObserver(cancelOnReturn) } }
      CustomTabsIntent.Builder().build().launchUrl(activity, Uri.parse(authUrl))
      val callbackUrl = deferred.await()
      if (callbackUrl == null) AuthResult.Canceled else AuthResult.Success(callbackUrl)
    } catch (t: Throwable) {
      AuthResult.Failure(t.message ?: "Authorization failed")
    } finally {
      AuthRedirectBus.pending = null
      lifecycle?.let {
        withContext(NonCancellable + Dispatchers.Main) { it.removeObserver(cancelOnReturn) }
      }
    }
  }

  actual fun consumeRedirectCallback(): String? = null
}

@Composable
actual fun rememberAuthorizationLauncher(): AuthorizationLauncher {
  val activity = LocalContext.current.findActivity()
  return remember(activity) {
    AuthorizationLauncher(
      activity = activity,
      redirectUri = "${GeneratedAuthConfig.REDIRECT_SCHEME}://${GeneratedAuthConfig.REDIRECT_HOST}",
    )
  }
}

private tailrec fun Context.findActivity(): Activity =
  when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> error("rememberAuthorizationLauncher must be called from an Activity context")
  }

/**
 * Receives the redirect (custom scheme deep link) and hands the callback URL to the awaiting
 * [AuthorizationLauncher]. Registered in the manifest with an intent-filter for
 * `${SCHEME}://${HOST}`.
 */
class LoginRedirectActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    intent?.data?.let { AuthRedirectBus.pending?.complete(it.toString()) }
    AuthRedirectBus.pending = null
    startActivity(
      Intent(this, MainActivity::class.java)
        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
    )
    finish()
  }
}
