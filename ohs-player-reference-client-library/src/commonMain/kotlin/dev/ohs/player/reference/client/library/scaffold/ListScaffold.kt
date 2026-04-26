package dev.ohs.player.reference.client.library.scaffold

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dev.ohs.player.reference.client.library.layout.VerticalListRenderer
import dev.ohs.player.reference.client.library.renderer.LayoutRenderer
import dev.ohs.player.reference.client.library.renderer.Renderer

@Composable
fun <T> ListScaffold(
    items: List<T>,
    item: Renderer<T>,
    onItemClick: (T) -> Unit,
    key: (T) -> Any,
    layout: LayoutRenderer<T> = remember { VerticalListRenderer() },
    topBar: @Composable () -> Unit = {},
    emptyState: @Composable () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        topBar()
        if (items.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                emptyState()
            }
        } else {
            layout.Render(
                items = items,
                itemRenderer = item,
                key = key,
                onItemClick = onItemClick,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
