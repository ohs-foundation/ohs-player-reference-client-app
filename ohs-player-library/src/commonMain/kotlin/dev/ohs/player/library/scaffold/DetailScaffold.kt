package dev.ohs.player.library.scaffold

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.ohs.player.library.registry.LocalViewRegistry
import dev.ohs.player.library.registry.ViewRegistry
import dev.ohs.player.library.registry.ViewType
import dev.ohs.player.library.registry.ViewTypeKey
import dev.ohs.player.library.renderer.ComponentRenderer
import kotlin.reflect.KClass

class DetailDslScope<T : Any> @PublishedApi internal constructor(
    @PublishedApi internal val registry: ViewRegistry,
    @PublishedApi internal val dataType: KClass<T>,
) {
    @PublishedApi internal var sections: List<ComponentRenderer<T>> = emptyList()
        private set

    @PublishedApi internal var topBar: (@Composable () -> Unit)? = null

    @PublishedApi internal var notFound: (@Composable () -> Unit)? = null

    /** Append a pre-built [ComponentRenderer]. Use this to reuse renderers across screens. */
    fun section(renderer: ComponentRenderer<T>) {
        sections += renderer
    }

    /** Append an inline section. The lambda receives the detail item. */
    fun section(content: @Composable (T) -> Unit) {
        sections += object : ComponentRenderer<T> {
            @Composable
            override fun Render(item: T, onClick: () -> Unit, modifier: Modifier) {
                content(item)
            }
        }
    }

    /** Resolve the section renderer from the registry by [viewType]. */
    fun section(viewType: ViewType) {
        sections += registry.getComponent(ViewTypeKey(viewType, dataType))
    }

    fun topBar(content: @Composable () -> Unit) {
        topBar = content
    }

    fun notFound(content: @Composable () -> Unit) {
        notFound = content
    }
}

@Composable
inline fun <reified T : Any> DetailScaffold(
    item: T?,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    sectionSpacing: Dp = 16.dp,
    modifier: Modifier = Modifier,
    builder: DetailDslScope<T>.() -> Unit,
) {
    val registry = LocalViewRegistry.current
    val scope = DetailDslScope(registry, T::class).apply(builder)
    Scaffold(
        modifier = modifier,
        topBar = scope.topBar ?: {},
    ) { padding ->
        if (item == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                scope.notFound?.invoke()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = contentPadding,
                verticalArrangement = Arrangement.spacedBy(sectionSpacing),
            ) {
                items(scope.sections) { section ->
                    section.Render(item = item, onClick = {}, modifier = Modifier)
                }
            }
        }
    }
}
