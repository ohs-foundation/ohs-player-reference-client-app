package dev.ohs.player.library.layout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.ohs.player.library.renderer.ConfiguredRenderer
import dev.ohs.player.library.renderer.LayoutRenderer

/**
 * Renders items vertically as a `LazyColumn`. The default layout for
 * [dev.ohs.player.library.scaffold.ListScaffold] when none is specified.
 *
 * View-type constants are typically declared once in an `object`; see
 * [dev.ohs.player.library.registry.ViewType].
 *
 * ```
 * object AppViewTypes {
 *     val VerticalList = ViewType("VerticalList")
 * }
 *
 * registry.registerLayout<PatientView>(
 *     AppViewTypes.VerticalList,
 *     VerticalListRenderer(
 *         contentPadding = PaddingValues(16.dp),
 *         itemSpacing = 12.dp,
 *     ),
 * )
 * ```
 *
 * @param contentPadding padding around the list content (not the items themselves).
 * @param itemSpacing vertical gap between items.
 */
class VerticalListRenderer<T>(
    private val contentPadding: PaddingValues = PaddingValues(0.dp),
    private val itemSpacing: Dp = 0.dp,
) : LayoutRenderer<T> {
    /** Lays out [items] in a `LazyColumn`, invoking [component] per item with [key]. */
    @Composable
    override fun Render(
        items: List<T>,
        component: ConfiguredRenderer<T>,
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
                component.Render(item, { onItemClick(item) }, Modifier)
            }
        }
    }
}
