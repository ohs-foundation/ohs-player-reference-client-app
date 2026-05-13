package dev.ohs.player.library.registry

import dev.ohs.player.library.renderer.ComponentRenderer
import dev.ohs.player.library.renderer.ConfiguredRenderer
import dev.ohs.player.library.renderer.LayoutRenderer
import dev.ohs.player.library.renderer.withConfig

class ViewRegistry {
    @PublishedApi
    internal class ComponentEntry<T : Any, C : Any>(
        val renderer: ComponentRenderer<T, C>,
        val config: C,
    ) {
        val configured: ConfiguredRenderer<T> = renderer.withConfig(config)
    }

    @PublishedApi
    internal val components = mutableMapOf<ViewTypeKey<*>, ComponentEntry<*, *>>()

    @PublishedApi
    internal val layouts = mutableMapOf<ViewTypeKey<*>, LayoutRenderer<*>>()

    @PublishedApi
    internal fun <T : Any, C : Any> putComponent(
        key: ViewTypeKey<T>,
        renderer: ComponentRenderer<T, C>,
        config: C,
    ) {
        components[key] = ComponentEntry(renderer, config)
    }

    @PublishedApi
    internal fun <T : Any> putLayout(key: ViewTypeKey<T>, renderer: LayoutRenderer<T>) {
        layouts[key] = renderer
    }

    @Suppress("UNCHECKED_CAST")
    @PublishedApi
    internal fun <T : Any> getComponent(key: ViewTypeKey<T>): ConfiguredRenderer<T> =
        (components[key] ?: missing("component", key)).configured as ConfiguredRenderer<T>

    @Suppress("UNCHECKED_CAST")
    @PublishedApi
    internal fun <T : Any> getComponentSource(key: ViewTypeKey<T>): ComponentRenderer<T, *> =
        (components[key] ?: missing("component", key)).renderer as ComponentRenderer<T, *>

    @Suppress("UNCHECKED_CAST")
    @PublishedApi
    internal fun <T : Any> getLayout(key: ViewTypeKey<T>): LayoutRenderer<T> {
        val renderer = layouts[key] ?: missing("layout", key)
        return renderer as LayoutRenderer<T>
    }

    private fun missing(kind: String, key: ViewTypeKey<*>): Nothing =
        throw NoSuchElementException(
            "No $kind renderer registered for " +
                "(${key.dataType.simpleName}, ${key.viewType.value}).",
        )
}

inline fun <reified T : Any, C : Any> ViewRegistry.registerComponent(
    viewType: ViewType,
    renderer: ComponentRenderer<T, C>,
    config: C,
) = putComponent(ViewTypeKey(viewType, T::class), renderer, config)

inline fun <reified T : Any> ViewRegistry.registerLayout(viewType: ViewType, renderer: LayoutRenderer<T>) =
    putLayout(ViewTypeKey(viewType, T::class), renderer)

inline fun <reified T : Any> ViewRegistry.componentRenderer(viewType: ViewType): ConfiguredRenderer<T> =
    getComponent(ViewTypeKey(viewType, T::class))

inline fun <reified T : Any> ViewRegistry.componentSource(viewType: ViewType): ComponentRenderer<T, *> =
    getComponentSource(ViewTypeKey(viewType, T::class))

inline fun <reified T : Any> ViewRegistry.layoutRenderer(viewType: ViewType): LayoutRenderer<T> =
    getLayout(ViewTypeKey(viewType, T::class))
