package dev.ohs.player.library.layout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.ohs.player.library.renderer.ConfiguredRenderer
import dev.ohs.player.library.renderer.LayoutRenderer

/**
 * Renders items in a vertically-scrolling `LazyVerticalGrid`.
 *
 * View-type constants are typically declared once in an `object`; see
 * [dev.ohs.player.library.registry.ViewType].
 *
 * ```
 * object AppViewTypes {
 *     val Grid = ViewType("Grid")
 * }
 *
 * registry.registerLayout<PatientView>(
 *     AppViewTypes.Grid,
 *     GridListRenderer(
 *         cells = GridCells.Fixed(2),
 *         contentPadding = PaddingValues(16.dp),
 *         itemSpacing = 12.dp,
 *     ),
 * )
 * ```
 *
 * @param cells column specification; defaults to two equal columns.
 * @param contentPadding padding around the grid content.
 * @param itemSpacing gap between items in both axes.
 */
class GridListRenderer<T>(
    private val cells: GridCells = GridCells.Fixed(2),
    private val contentPadding: PaddingValues = PaddingValues(0.dp),
    private val itemSpacing: Dp = 0.dp,
) : LayoutRenderer<T> {
    /** Lays out [items] in a `LazyVerticalGrid`, invoking [component] per item with [key]. */
    @Composable
    override fun Render(
        items: List<T>,
        component: ConfiguredRenderer<T>,
        key: (T) -> Any,
        onItemClick: (T) -> Unit,
        modifier: Modifier,
    ) {
        LazyVerticalGrid(
            columns = cells,
            modifier = modifier,
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(itemSpacing),
            horizontalArrangement = Arrangement.spacedBy(itemSpacing),
        ) {
            items(items = items, key = key) { item ->
                component.Render(item, { onItemClick(item) }, Modifier)
            }
        }
    }
}
