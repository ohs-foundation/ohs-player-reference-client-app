package dev.ohs.player.reference.client.library.renderer

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

interface Renderer<T> {
    @Composable
    fun Render(
        item: T,
        onClick: () -> Unit = {},
        modifier: Modifier = Modifier,
    )
}
