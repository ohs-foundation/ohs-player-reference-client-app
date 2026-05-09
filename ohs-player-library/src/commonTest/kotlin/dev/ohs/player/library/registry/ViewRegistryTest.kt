package dev.ohs.player.library.registry

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.ohs.player.library.renderer.LayoutRenderer
import dev.ohs.player.library.renderer.Renderer
import kotlin.test.Test
import kotlin.test.assertNull
import kotlin.test.assertSame

private val FooViewType = ViewType("Foo")

private class StringRenderer : Renderer<String> {
    @Composable override fun Render(item: String, onClick: () -> Unit, modifier: Modifier) {}
}

private class StringLayoutRenderer : LayoutRenderer<String> {
    @Composable override fun Render(
        items: List<String>,
        itemRenderer: Renderer<String>,
        key: (String) -> Any,
        onItemClick: (String) -> Unit,
        modifier: Modifier,
    ) {}
}

class ViewRegistryTest {

    @Test
    fun registerAndLookup_works_forItemAndLayout() {
        val registry = ViewRegistry()
        val item = StringRenderer()
        val layout = StringLayoutRenderer()

        registry.registerItem<String>(FooViewType, item)
        registry.registerLayout<String>(FooViewType, layout)

        assertSame(item, registry.itemRenderer<String>(FooViewType))
        assertSame(layout, registry.layoutRenderer<String>(FooViewType))
    }

    @Test
    fun differentDataType_isDifferentKey() {
        val registry = ViewRegistry()
        registry.registerItem<String>(FooViewType, StringRenderer())

        // Same view-type value, different T, must miss; protects against accidental key
        // collisions if T is ever dropped from the lookup key.
        assertNull(registry.itemRenderer<Int>(FooViewType))
    }
}
