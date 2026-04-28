package dev.ohs.player.library.registry

import dev.ohs.player.library.renderer.LayoutRenderer
import dev.ohs.player.library.renderer.Renderer

class ViewRegistry {
    @PublishedApi
    internal val items = mutableMapOf<ViewTypeKey<*>, Renderer<*>>()

    @PublishedApi
    internal val layouts = mutableMapOf<ViewTypeKey<*>, LayoutRenderer<*>>()

    @PublishedApi
    internal fun <T : Any> putItem(key: ViewTypeKey<T>, renderer: Renderer<T>) {
        items[key] = renderer
    }

    @PublishedApi
    internal fun <T : Any> putLayout(key: ViewTypeKey<T>, renderer: LayoutRenderer<T>) {
        layouts[key] = renderer
    }

    @Suppress("UNCHECKED_CAST")
    @PublishedApi
    internal fun <T : Any> getItem(key: ViewTypeKey<T>): Renderer<T>? =
        items[key] as? Renderer<T>

    @Suppress("UNCHECKED_CAST")
    @PublishedApi
    internal fun <T : Any> getLayout(key: ViewTypeKey<T>): LayoutRenderer<T>? =
        layouts[key] as? LayoutRenderer<T>
}

inline fun <reified T : Any> ViewRegistry.registerItem(viewType: ViewType, renderer: Renderer<T>) =
    putItem(ViewTypeKey(viewType, T::class), renderer)

inline fun <reified T : Any> ViewRegistry.registerLayout(viewType: ViewType, renderer: LayoutRenderer<T>) =
    putLayout(ViewTypeKey(viewType, T::class), renderer)

inline fun <reified T : Any> ViewRegistry.itemRenderer(viewType: ViewType): Renderer<T>? =
    getItem(ViewTypeKey(viewType, T::class))

inline fun <reified T : Any> ViewRegistry.layoutRenderer(viewType: ViewType): LayoutRenderer<T>? =
    getLayout(ViewTypeKey(viewType, T::class))
