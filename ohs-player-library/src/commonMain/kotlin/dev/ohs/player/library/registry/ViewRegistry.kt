package dev.ohs.player.library.registry

import dev.ohs.player.library.renderer.ComponentRenderer
import dev.ohs.player.library.renderer.ConfiguredRenderer
import dev.ohs.player.library.renderer.LayoutRenderer
import dev.ohs.player.library.renderer.withConfig

class ViewRegistry {
    @PublishedApi
    internal val components = mutableMapOf<ViewTypeKey<*>, ConfiguredRenderer<*>>()

    @PublishedApi
    internal val layouts = mutableMapOf<ViewTypeKey<*>, LayoutRenderer<*>>()

    @PublishedApi
    internal fun <T : Any> putComponent(key: ViewTypeKey<T>, renderer: ConfiguredRenderer<T>) {
        components[key] = renderer
    }

    @PublishedApi
    internal fun <T : Any> putLayout(key: ViewTypeKey<T>, renderer: LayoutRenderer<T>) {
        layouts[key] = renderer
    }

    @Suppress("UNCHECKED_CAST")
    @PublishedApi
    internal fun <T : Any> getComponent(key: ViewTypeKey<T>): ConfiguredRenderer<T> {
        val renderer = components[key]
            ?: throw NoSuchElementException(
                "No component renderer registered for " +
                    "(${key.dataType.simpleName}, ${key.viewType.value}).",
            )
        return renderer as ConfiguredRenderer<T>
    }

    @Suppress("UNCHECKED_CAST")
    @PublishedApi
    internal fun <T : Any> getLayout(key: ViewTypeKey<T>): LayoutRenderer<T> {
        val renderer = layouts[key]
            ?: throw NoSuchElementException(
                "No layout renderer registered for " +
                    "(${key.dataType.simpleName}, ${key.viewType.value}).",
            )
        return renderer as LayoutRenderer<T>
    }
}

inline fun <reified T : Any, C : Any> ViewRegistry.registerComponent(
    viewType: ViewType,
    renderer: ComponentRenderer<T, C>,
    config: C,
) = putComponent(ViewTypeKey(viewType, T::class), renderer.withConfig(config))

inline fun <reified T : Any> ViewRegistry.registerLayout(viewType: ViewType, renderer: LayoutRenderer<T>) =
    putLayout(ViewTypeKey(viewType, T::class), renderer)

inline fun <reified T : Any> ViewRegistry.componentRenderer(viewType: ViewType): ConfiguredRenderer<T> =
    getComponent(ViewTypeKey(viewType, T::class))

inline fun <reified T : Any> ViewRegistry.layoutRenderer(viewType: ViewType): LayoutRenderer<T> =
    getLayout(ViewTypeKey(viewType, T::class))
