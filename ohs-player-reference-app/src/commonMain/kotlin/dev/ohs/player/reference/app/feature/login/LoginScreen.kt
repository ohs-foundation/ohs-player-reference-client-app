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
package dev.ohs.player.reference.app.feature.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ohsplayerreferenceclientapp.ohs_player_reference_app.generated.resources.Res
import ohsplayerreferenceclientapp.ohs_player_reference_app.generated.resources.app_logo
import ohsplayerreferenceclientapp.ohs_player_reference_app.generated.resources.login_brand
import ohsplayerreferenceclientapp.ohs_player_reference_app.generated.resources.login_card_title
import ohsplayerreferenceclientapp.ohs_player_reference_app.generated.resources.login_error_dismiss
import ohsplayerreferenceclientapp.ohs_player_reference_app.generated.resources.login_error_title
import ohsplayerreferenceclientapp.ohs_player_reference_app.generated.resources.login_redirect_hint
import ohsplayerreferenceclientapp.ohs_player_reference_app.generated.resources.login_sign_in
import ohsplayerreferenceclientapp.ohs_player_reference_app.generated.resources.login_subtitle
import ohsplayerreferenceclientapp.ohs_player_reference_app.generated.resources.login_tagline
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** Material 3's "expanded" window size class breakpoint (matches HomeWidthBreakpoint.kt). */
private val LOGIN_EXPANDED_WIDTH_BREAKPOINT = 840.dp

/**
 * PKCE redirect login — no password form; the primary action hands off to the identity provider.
 * Branches on width (never on platform), matching this app's existing manual-breakpoint convention
 * (`HomeWidthBreakpoint.kt`): Expanded (≥840dp) shows a brand panel beside a centered sign-in card;
 * Compact/Medium show a brand band over a sign-in sheet.
 */
@Composable
fun LoginScreen(
  signingIn: Boolean,
  error: String?,
  onSignIn: () -> Unit,
  onErrorDismiss: () -> Unit,
) {
  BoxWithConstraints {
    if (maxWidth >= LOGIN_EXPANDED_WIDTH_BREAKPOINT) {
      ExpandedLogin(signingIn, onSignIn)
    } else {
      CompactLogin(signingIn, onSignIn)
    }
  }

  if (error != null) {
    LoginErrorDialog(message = error, onDismiss = onErrorDismiss)
  }
}

/** Expanded: brand panel (left) + centered sign-in card on a grey field (right). */
@Composable
private fun ExpandedLogin(signingIn: Boolean, onSignIn: () -> Unit) {
  Row(Modifier.fillMaxSize()) {
    Box(Modifier.weight(1f).fillMaxHeight().background(MaterialTheme.colorScheme.primary)) {
      Column(
        modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(48.dp),
        verticalArrangement = Arrangement.SpaceBetween,
      ) {
        Wordmark(MaterialTheme.colorScheme.onPrimary)
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
          Text(
            text = stringResource(Res.string.login_tagline),
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onPrimary,
          )
          Box(
            Modifier.width(56.dp)
              .height(4.dp)
              .background(MaterialTheme.colorScheme.secondary, RoundedCornerShape(2.dp))
          )
          Text(
            text = stringResource(Res.string.login_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f),
          )
        }
      }
    }

    Box(
      modifier =
        Modifier.weight(1f)
          .fillMaxHeight()
          .background(MaterialTheme.colorScheme.background)
          .safeDrawingPadding()
          .padding(32.dp),
      contentAlignment = Alignment.Center,
    ) {
      Surface(
        modifier = Modifier.widthIn(max = 424.dp).fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp,
      ) {
        Column(
          modifier = Modifier.padding(40.dp),
          verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Image(
              painter = painterResource(Res.drawable.app_logo),
              contentDescription = null,
              modifier = Modifier.size(28.dp),
            )
            Text(
              text = stringResource(Res.string.login_brand),
              style = MaterialTheme.typography.labelLarge,
              color = MaterialTheme.colorScheme.primary,
              fontWeight = FontWeight.Bold,
            )
          }
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
              stringResource(Res.string.login_card_title),
              style = MaterialTheme.typography.headlineSmall,
            )
            Text(
              text = stringResource(Res.string.login_redirect_hint),
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          SignInButton(signingIn, onSignIn)
        }
      }
    }
  }
}

/** Compact/Medium: brand band over a rounded sign-in sheet; button pinned low. */
@Composable
private fun CompactLogin(signingIn: Boolean, onSignIn: () -> Unit) {
  Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .background(MaterialTheme.colorScheme.primary)
          .statusBarsPadding()
          .padding(horizontal = 32.dp, vertical = 32.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Wordmark(MaterialTheme.colorScheme.onPrimary)
      Text(
        text = stringResource(Res.string.login_tagline),
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onPrimary,
        textAlign = TextAlign.Center,
      )
      Text(
        text = stringResource(Res.string.login_subtitle),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f),
        textAlign = TextAlign.Center,
      )
    }

    Column(
      modifier =
        Modifier.fillMaxWidth()
          .weight(1f)
          .background(
            MaterialTheme.colorScheme.surface,
            RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
          )
          .navigationBarsPadding()
          .padding(horizontal = 24.dp)
          .padding(top = 28.dp, bottom = 20.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Text(
        stringResource(Res.string.login_card_title),
        style = MaterialTheme.typography.headlineSmall,
      )
      Text(
        text = stringResource(Res.string.login_redirect_hint),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Spacer(Modifier.weight(1f))
      SignInButton(signingIn, onSignIn)
      Spacer(Modifier.height(8.dp))
    }
  }
}

@Composable
private fun Wordmark(color: androidx.compose.ui.graphics.Color) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Image(
      painter = painterResource(Res.drawable.app_logo),
      contentDescription = null,
      modifier = Modifier.size(32.dp),
    )
    Text(
      text = stringResource(Res.string.login_brand),
      style = MaterialTheme.typography.titleLarge,
      color = color,
      fontWeight = FontWeight.Bold,
    )
  }
}

@Composable
private fun SignInButton(signingIn: Boolean, onSignIn: () -> Unit) {
  Button(
    onClick = onSignIn,
    enabled = !signingIn,
    shape = RoundedCornerShape(8.dp),
    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
    modifier = Modifier.fillMaxWidth(),
  ) {
    if (signingIn) {
      CircularProgressIndicator(
        modifier = Modifier.size(18.dp),
        strokeWidth = 2.dp,
        color = MaterialTheme.colorScheme.onPrimary,
      )
    } else {
      Text(stringResource(Res.string.login_sign_in).uppercase())
    }
  }
}

@Composable
private fun LoginErrorDialog(message: String, onDismiss: () -> Unit) {
  AlertDialog(
    onDismissRequest = onDismiss,
    icon = {
      Icon(
        imageVector = Icons.Filled.Warning,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.error,
        modifier = Modifier.size(48.dp),
      )
    },
    title = {
      Text(
        text = stringResource(Res.string.login_error_title),
        color = MaterialTheme.colorScheme.error,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
      )
    },
    text = {
      Text(text = message, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    },
    confirmButton = {
      TextButton(onClick = onDismiss) { Text(stringResource(Res.string.login_error_dismiss)) }
    },
    iconContentColor = MaterialTheme.colorScheme.error,
    titleContentColor = MaterialTheme.colorScheme.error,
  )
}
