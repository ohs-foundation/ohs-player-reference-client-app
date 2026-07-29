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
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ohsplayerreferenceclientapp.ohs_player_reference_app.generated.resources.Res
import ohsplayerreferenceclientapp.ohs_player_reference_app.generated.resources.app_logo
import ohsplayerreferenceclientapp.ohs_player_reference_app.generated.resources.login_brand
import ohsplayerreferenceclientapp.ohs_player_reference_app.generated.resources.login_card_title
import ohsplayerreferenceclientapp.ohs_player_reference_app.generated.resources.login_error_dismiss
import ohsplayerreferenceclientapp.ohs_player_reference_app.generated.resources.login_error_title
import ohsplayerreferenceclientapp.ohs_player_reference_app.generated.resources.login_redirect_hint
import ohsplayerreferenceclientapp.ohs_player_reference_app.generated.resources.login_sign_in
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** Material 3's "expanded" window size class breakpoint (matches HomeWidthBreakpoint.kt). */
private val LOGIN_EXPANDED_WIDTH_BREAKPOINT = 840.dp

/**
 * PKCE redirect login — no password form; the primary action hands off to the identity provider.
 * Branches on width (never on platform): Expanded (>= 840dp) sets a solid brand panel beside a flat
 * sign-in side; Compact/Medium stack a brand band over a rounded sign-in sheet. Both are flat — the
 * brand color carries the identity, so there is no elevated card.
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

/** Expanded: solid brand panel (left) beside a flat sign-in side (right). */
@Composable
private fun ExpandedLogin(signingIn: Boolean, onSignIn: () -> Unit) {
  Row(Modifier.fillMaxSize()) {
    Column(
      modifier =
        Modifier.weight(5f)
          .fillMaxHeight()
          .background(MaterialTheme.colorScheme.primary)
          .safeDrawingPadding()
          .padding(44.dp)
    ) {
      MonoLogoMark(64.dp)
      Spacer(Modifier.height(22.dp))
      Text(
        text = stringResource(Res.string.login_brand),
        style = MaterialTheme.typography.displaySmall,
        color = MaterialTheme.colorScheme.onPrimary,
        fontWeight = FontWeight.Bold,
      )
    }

    Box(
      modifier =
        Modifier.weight(6f)
          .fillMaxHeight()
          .background(MaterialTheme.colorScheme.surface)
          .safeDrawingPadding()
          .padding(horizontal = 48.dp),
      contentAlignment = Alignment.Center,
    ) {
      Column(modifier = Modifier.widthIn(max = 420.dp).fillMaxWidth()) {
        SignInContent(signingIn, onSignIn, showBrandRow = true)
      }
    }
  }
}

/** Compact/Medium: brand band over a rounded sign-in sheet; button pinned low. */
@Composable
private fun CompactLogin(signingIn: Boolean, onSignIn: () -> Unit) {
  Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .background(MaterialTheme.colorScheme.primary)
          .statusBarsPadding()
          .padding(horizontal = 28.dp, vertical = 44.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      MonoLogoMark(56.dp)
      Text(
        text = stringResource(Res.string.login_brand),
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onPrimary,
        fontWeight = FontWeight.Bold,
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
          .padding(horizontal = 26.dp)
          .padding(top = 32.dp, bottom = 22.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      SignInContent(signingIn, onSignIn, showBrandRow = false, buttonAtBottom = true)
    }
  }
}

@Composable
private fun ColumnScope.SignInContent(
  signingIn: Boolean,
  onSignIn: () -> Unit,
  showBrandRow: Boolean,
  buttonAtBottom: Boolean = false,
) {
  if (showBrandRow) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Image(
        painter = painterResource(Res.drawable.app_logo),
        contentDescription = null,
        modifier = Modifier.size(24.dp),
      )
      Text(
        text = stringResource(Res.string.login_brand).uppercase(),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
      )
    }
    Spacer(Modifier.height(26.dp))
  }

  Text(
    text = stringResource(Res.string.login_card_title),
    style = MaterialTheme.typography.headlineMedium,
  )
  Spacer(Modifier.height(10.dp))
  Text(
    text = stringResource(Res.string.login_redirect_hint),
    style = MaterialTheme.typography.bodyLarge,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
  )

  if (buttonAtBottom) {
    Spacer(Modifier.weight(1f))
  } else {
    Spacer(Modifier.height(28.dp))
  }
  SignInButton(signingIn, onSignIn)
}

@Composable
private fun SignInButton(signingIn: Boolean, onSignIn: () -> Unit) {
  Button(
    onClick = onSignIn,
    enabled = !signingIn,
    shape = RoundedCornerShape(14.dp),
    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
    modifier = Modifier.fillMaxWidth().height(52.dp),
  ) {
    if (signingIn) {
      CircularProgressIndicator(
        modifier = Modifier.size(18.dp),
        strokeWidth = 2.dp,
        color = MaterialTheme.colorScheme.onPrimary,
      )
    } else {
      Text(
        text = stringResource(Res.string.login_sign_in),
        style = MaterialTheme.typography.titleMedium,
      )
      Spacer(Modifier.size(8.dp))
      Icon(
        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
        contentDescription = null,
        modifier = Modifier.size(18.dp),
      )
    }
  }
}

/** The 2x2 brand mark rendered monochrome for the solid brand panel. */
@Composable
private fun MonoLogoMark(size: Dp) {
  val gap = size * 0.09f
  val cell = (size - gap) / 2
  val corner = cell * 0.28f
  val alphas = listOf(0.9f, 0.28f, 0.28f, 0.55f)
  val cellModifier = { index: Int ->
    Modifier.size(cell)
      .clip(RoundedCornerShape(corner))
      .background(Color.White.copy(alpha = alphas[index]))
  }
  Column(verticalArrangement = Arrangement.spacedBy(gap)) {
    Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
      Box(cellModifier(0))
      Box(cellModifier(1))
    }
    Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
      Box(cellModifier(2))
      Box(cellModifier(3))
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
