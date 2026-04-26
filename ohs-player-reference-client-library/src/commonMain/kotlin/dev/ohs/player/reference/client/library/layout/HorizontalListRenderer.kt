package dev.ohs.player.reference.client.library.layout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.ohs.player.reference.client.library.renderer.LayoutRenderer
import dev.ohs.player.reference.client.library.renderer.Renderer

class HorizontalListRenderer<T>(
    private val contentPadding: PaddingValues = PaddingValues(0.dp),
    private val itemSpacing: Dp = 0.dp,
) : LayoutRenderer<T> {
    @Composable
    override fun Render(
        items: List<T>,
        itemRenderer: Renderer<T>,
        key: (T) -> Any,
        onItemClick: (T) -> Unit,
        modifier: Modifier,
    ) {
        LazyRow(
            modifier = modifier,
            contentPadding = contentPadding,
            horizontalArrangement = Arrangement.spacedBy(itemSpacing),
        ) {
            items(items = items, key = key) { item ->
                itemRenderer.Render(
                    item = item,
                    onClick = { onItemClick(item) },
                    modifier = Modifier,
                )
            }
        }
    }
}
