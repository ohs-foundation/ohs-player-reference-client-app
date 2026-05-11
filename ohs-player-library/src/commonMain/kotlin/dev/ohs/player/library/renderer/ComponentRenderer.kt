package dev.ohs.player.library.renderer

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

interface ComponentRenderer<T, C> {
    @Composable
    fun Render(item: T, config: C, onClick: () -> Unit = {}, modifier: Modifier = Modifier)
}

/** Captures [boundConfig] in a closure so the result can be stored / passed without exposing [C]. */
@PublishedApi
internal fun <T : Any, C : Any> ComponentRenderer<T, C>.withConfig(boundConfig: C): ComponentRenderer<T, Unit> {
    val source = this
    return object : ComponentRenderer<T, Unit> {
        @Composable
        override fun Render(item: T, config: Unit, onClick: () -> Unit, modifier: Modifier) =
            source.Render(item, boundConfig, onClick, modifier)
    }
}
