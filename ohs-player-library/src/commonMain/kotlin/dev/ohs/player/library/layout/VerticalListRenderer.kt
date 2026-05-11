package dev.ohs.player.library.layout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.ohs.player.library.renderer.ComponentRenderer
import dev.ohs.player.library.renderer.LayoutRenderer

class VerticalListRenderer<T>(
    private val contentPadding: PaddingValues = PaddingValues(0.dp),
    private val itemSpacing: Dp = 0.dp,
) : LayoutRenderer<T> {
    @Composable
    override fun Render(
        items: List<T>,
        component: ComponentRenderer<T, Unit>,
        key: (T) -> Any,
        onItemClick: (T) -> Unit,
        modifier: Modifier,
    ) {
        LazyColumn(
            modifier = modifier,
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(itemSpacing),
        ) {
            items(items = items, key = key) { item ->
                component.Render(
                    item = item,
                    config = Unit,
                    onClick = { onItemClick(item) },
                    modifier = Modifier,
                )
            }
        }
    }
}
