package dev.ohs.player.library.renderer

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Arranges a `List<T>` and delegates per-item rendering to a [ConfiguredRenderer].
 *
 * The library ships three implementations —
 * [dev.ohs.player.library.layout.VerticalListRenderer],
 * [dev.ohs.player.library.layout.HorizontalListRenderer], and
 * [dev.ohs.player.library.layout.GridListRenderer] — but apps can supply their own.
 *
 * ```
 * class StaggeredRenderer<T> : LayoutRenderer<T> {
 *     @Composable
 *     override fun Render(items, component, key, onItemClick, modifier) {
 *         LazyVerticalStaggeredGrid(StaggeredGridCells.Fixed(2), modifier) {
 *             items(items, key) { item ->
 *                 component.Render(item, { onItemClick(item) }, Modifier)
 *             }
 *         }
 *     }
 * }
 * ```
 */
interface LayoutRenderer<T> {
    /**
     * Lays out [items] using [component] for each entry.
     *
     * @param items the data to render.
     * @param component the bound per-item renderer.
     * @param key stable key function used by the underlying lazy list.
     * @param onItemClick invoked when the user taps an item.
     * @param modifier applied to the outer container.
     */
    @Composable
    fun Render(
        items: List<T>,
        component: ConfiguredRenderer<T>,
        key: (T) -> Any,
        onItemClick: (T) -> Unit,
        modifier: Modifier = Modifier,
    )
}
