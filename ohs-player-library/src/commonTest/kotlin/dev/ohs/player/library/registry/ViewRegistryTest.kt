package dev.ohs.player.library.registry

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.ohs.player.library.renderer.ComponentRenderer
import dev.ohs.player.library.renderer.LayoutRenderer
import kotlin.test.Test
import kotlin.test.assertNull
import kotlin.test.assertSame

private val FooViewType = ViewType("Foo")

private class StringRenderer : ComponentRenderer<String> {
    @Composable override fun Render(item: String, onClick: () -> Unit, modifier: Modifier) {}
}

private class StringLayoutRenderer : LayoutRenderer<String> {
    @Composable override fun Render(
        items: List<String>,
        component: ComponentRenderer<String>,
        key: (String) -> Any,
        onItemClick: (String) -> Unit,
        modifier: Modifier,
    ) {}
}

class ViewRegistryTest {

    @Test
    fun registerAndLookup_works_forItemAndLayout() {
        val registry = ViewRegistry()
        val component = StringRenderer()
        val layout = StringLayoutRenderer()

        registry.registerComponent<String>(FooViewType, component)
        registry.registerLayout<String>(FooViewType, layout)

        assertSame(component, registry.componentRenderer<String>(FooViewType))
        assertSame(layout, registry.layoutRenderer<String>(FooViewType))
    }

    @Test
    fun differentDataType_isDifferentKey() {
        val registry = ViewRegistry()
        registry.registerComponent<String>(FooViewType, StringRenderer())

        // Same view-type value, different T, must miss; protects against accidental key
        // collisions if T is ever dropped from the lookup key.
        assertNull(registry.componentRenderer<Int>(FooViewType))
    }
}
