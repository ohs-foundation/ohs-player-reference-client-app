package dev.ohs.player.library.renderer

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

interface ComponentRenderer<T, C> {
    @Composable
    fun Render(item: T, config: C, onClick: () -> Unit = {}, modifier: Modifier = Modifier)
}

/**
 * The bound form of a renderer — config already applied. A `fun interface` so callers can
 * supply a lambda (SAM conversion) without naming the type, while the runtime class is
 * stable for storage and casts.
 */
fun interface ConfiguredRenderer<T> {
    @Composable
    fun Render(item: T, onClick: () -> Unit, modifier: Modifier)
}

/** Captures [boundConfig] in a closure, producing the bound form for registry storage. */
@PublishedApi
internal fun <T : Any, C : Any> ComponentRenderer<T, C>.withConfig(boundConfig: C): ConfiguredRenderer<T> {
    val source = this
    return ConfiguredRenderer { item, onClick, modifier -> source.Render(item, boundConfig, onClick, modifier) }
}
