package dev.ohs.player.reference.client.app.feature.component.common

import androidx.compose.runtime.Composable

class CardDslScope {
    internal var headerContent: (@Composable () -> Unit)? = null
    internal var bodyContent: (@Composable () -> Unit)? = null
    internal var footerContent: (@Composable () -> Unit)? = null

    fun header(content: @Composable () -> Unit) {
        headerContent = content
    }

    fun body(content: @Composable () -> Unit) {
        bodyContent = content
    }

    fun footer(content: @Composable () -> Unit) {
        footerContent = content
    }
}
