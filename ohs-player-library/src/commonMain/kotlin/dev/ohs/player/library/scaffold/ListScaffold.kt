package dev.ohs.player.library.scaffold

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dev.ohs.player.library.layout.VerticalListRenderer
import dev.ohs.player.library.registry.LocalViewRegistry
import dev.ohs.player.library.registry.ViewRegistry
import dev.ohs.player.library.registry.ViewType
import dev.ohs.player.library.registry.ViewTypeKey
import dev.ohs.player.library.renderer.ComponentRenderer
import dev.ohs.player.library.renderer.LayoutRenderer
import dev.ohs.player.library.renderer.withConfig
import kotlin.reflect.KClass

class ListDslScope<T : Any> @PublishedApi internal constructor(
    @PublishedApi internal val registry: ViewRegistry,
    @PublishedApi internal val dataType: KClass<T>,
) {
    @PublishedApi internal var component: ComponentRenderer<T, Unit>? = null

    @PublishedApi internal var layout: LayoutRenderer<T>? = null

    @PublishedApi internal var topBar: (@Composable () -> Unit)? = null

    @PublishedApi internal var emptyState: (@Composable () -> Unit)? = null

    /** Set the component renderer using a pre-built [ComponentRenderer] and its config. Required. */
    fun <C : Any> component(renderer: ComponentRenderer<T, C>, config: C) {
        component = renderer.withConfig(config)
    }

    /** Set the component renderer using an inline composable. Required. */
    fun component(content: @Composable (T, onClick: () -> Unit) -> Unit) {
        component = object : ComponentRenderer<T, Unit> {
            @Composable
            override fun Render(item: T, config: Unit, onClick: () -> Unit, modifier: Modifier) {
                content(item, onClick)
            }
        }
    }

    /** Resolve the component renderer from the registry by [viewType]. */
    fun component(viewType: ViewType) {
        component = registry.getComponent(ViewTypeKey(viewType, dataType))
    }

    /** Override the default vertical layout. */
    fun layout(renderer: LayoutRenderer<T>) {
        layout = renderer
    }

    /** Resolve the layout renderer from the registry by [viewType]. */
    fun layout(viewType: ViewType) {
        layout = registry.getLayout(ViewTypeKey(viewType, dataType))
    }

    fun topBar(content: @Composable () -> Unit) {
        topBar = content
    }

    fun emptyState(content: @Composable () -> Unit) {
        emptyState = content
    }
}

@Composable
inline fun <reified T : Any> ListScaffold(
    items: List<T>,
    noinline onItemClick: (T) -> Unit,
    noinline key: (T) -> Any,
    modifier: Modifier = Modifier,
    builder: ListDslScope<T>.() -> Unit,
) {
    val registry = LocalViewRegistry.current
    val scope = ListDslScope(registry, T::class).apply(builder)
    val component = requireNotNull(scope.component) {
        "ListScaffold requires component(...) to be called in the builder."
    }
    val defaultLayout = remember { VerticalListRenderer<T>() }
    val layoutRenderer = scope.layout ?: defaultLayout

    Scaffold(
        modifier = modifier,
        topBar = scope.topBar ?: {},
    ) { padding ->
        if (items.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                scope.emptyState?.invoke()
            }
        } else {
            layoutRenderer.Render(
                items = items,
                component = component,
                key = key,
                onItemClick = onItemClick,
                modifier = Modifier.fillMaxSize().padding(padding),
            )
        }
    }
}
