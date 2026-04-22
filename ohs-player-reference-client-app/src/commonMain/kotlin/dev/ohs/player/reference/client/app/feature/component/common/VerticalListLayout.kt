package dev.ohs.player.reference.client.app.feature.component.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.ohs.player.reference.client.library.registry.LayoutViewFactory

class VerticalListLayout<T> : LayoutViewFactory<T> {
    @Composable
    override fun Content(
        items: List<T>,
        key: (T) -> Any,
        itemContent: @Composable (T) -> Unit,
        modifier: Modifier,
    ) {
        LazyColumn(
            modifier = modifier,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(items, key = key) { item ->
                itemContent(item)
            }
        }
    }
}
