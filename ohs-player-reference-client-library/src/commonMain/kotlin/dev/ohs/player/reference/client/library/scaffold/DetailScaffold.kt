package dev.ohs.player.reference.client.library.scaffold

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.ohs.player.reference.client.library.renderer.Renderer

class DetailDslScope<T> {
    internal val sections = mutableListOf<Renderer<T>>()
    internal var topBar: (@Composable () -> Unit)? = null
    internal var notFound: (@Composable () -> Unit)? = null

    /** Append a pre-built [Renderer]. Use this to reuse renderers across screens. */
    fun section(renderer: Renderer<T>) {
        sections += renderer
    }

    /** Append an inline section. The lambda receives the detail item. */
    fun section(content: @Composable (T) -> Unit) {
        sections += object : Renderer<T> {
            @Composable
            override fun Render(item: T, onClick: () -> Unit, modifier: Modifier) {
                content(item)
            }
        }
    }

    fun topBar(content: @Composable () -> Unit) {
        topBar = content
    }

    fun notFound(content: @Composable () -> Unit) {
        notFound = content
    }
}

@Composable
fun <T> DetailScaffold(
    item: T?,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    sectionSpacing: Dp = 16.dp,
    modifier: Modifier = Modifier,
    builder: DetailDslScope<T>.() -> Unit,
) {
    val scope = DetailDslScope<T>().apply(builder)
    Column(modifier = modifier.fillMaxSize()) {
        scope.topBar?.invoke()
        if (item == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                scope.notFound?.invoke()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(contentPadding),
                verticalArrangement = Arrangement.spacedBy(sectionSpacing),
            ) {
                scope.sections.forEach { section ->
                    section.Render(item = item, onClick = {}, modifier = Modifier)
                }
            }
        }
    }
}
