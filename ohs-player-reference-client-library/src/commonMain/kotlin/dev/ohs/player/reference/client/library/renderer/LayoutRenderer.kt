package dev.ohs.player.reference.client.library.renderer

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

interface LayoutRenderer<T> {
    @Composable
    fun Render(
        items: List<T>,
        itemRenderer: Renderer<T>,
        key: (T) -> Any,
        onItemClick: (T) -> Unit,
        modifier: Modifier = Modifier,
    )
}
