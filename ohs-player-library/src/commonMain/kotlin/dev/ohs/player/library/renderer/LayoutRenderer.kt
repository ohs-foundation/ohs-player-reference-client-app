package dev.ohs.player.library.renderer

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

interface LayoutRenderer<T> {
    @Composable
    fun Render(
        items: List<T>,
        component: ComponentRenderer<T>,
        key: (T) -> Any,
        onItemClick: (T) -> Unit,
        modifier: Modifier = Modifier,
    )
}
