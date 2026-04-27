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

class ListDslScope<T> {
    internal var item: Renderer<T>? = null
    internal var layout: LayoutRenderer<T>? = null
    internal var topBar: (@Composable () -> Unit)? = null
    internal var emptyState: (@Composable () -> Unit)? = null

    /** Set the item renderer using a pre-built [Renderer]. Required. */
    fun item(renderer: Renderer<T>) {
        item = renderer
    }

    /** Set the item renderer using an inline composable. Required. */
    fun item(content: @Composable (T, onClick: () -> Unit) -> Unit) {
        item = object : Renderer<T> {
            @Composable
            override fun Render(item: T, onClick: () -> Unit, modifier: Modifier) {
                content(item, onClick)
            }
        }
    }

    /** Override the default vertical layout. */
    fun layout(renderer: LayoutRenderer<T>) {
        layout = renderer
    }

    fun topBar(content: @Composable () -> Unit) {
        topBar = content
    }

    fun emptyState(content: @Composable () -> Unit) {
        emptyState = content
    }
}

@Composable
fun <T> ListScaffold(
    items: List<T>,
    onItemClick: (T) -> Unit,
    key: (T) -> Any,
    modifier: Modifier = Modifier,
    builder: ListDslScope<T>.() -> Unit,
) {
    val scope = ListDslScope<T>().apply(builder)
    val itemRenderer = requireNotNull(scope.item) {
        "ListScaffold requires item(...) to be called in the builder."
    }
    val defaultLayout = remember { VerticalListRenderer<T>() }
    val layoutRenderer = scope.layout ?: defaultLayout

    Column(modifier = modifier.fillMaxSize()) {
        scope.topBar?.invoke()
        if (items.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                scope.emptyState?.invoke()
            }
        } else {
            layoutRenderer.Render(
                items = items,
                itemRenderer = itemRenderer,
                key = key,
                onItemClick = onItemClick,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
