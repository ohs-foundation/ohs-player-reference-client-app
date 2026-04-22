package dev.ohs.player.reference.client.library.registry

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

fun interface LayoutViewFactory<T> {
    @Composable
    fun Content(
        items: List<T>,
        key: (T) -> Any,
        itemContent: @Composable (T) -> Unit,
        modifier: Modifier,
    )
}
