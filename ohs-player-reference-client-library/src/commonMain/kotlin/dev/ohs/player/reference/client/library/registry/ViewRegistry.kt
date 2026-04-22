package dev.ohs.player.reference.client.library.registry

class ViewRegistry {
    private val listItemFactories = mutableMapOf<ViewType, ListItemViewFactory<*>>()
    private val layoutFactories = mutableMapOf<ViewType, LayoutViewFactory<*>>()

    fun <T> registerListItem(type: ViewType, factory: ListItemViewFactory<T>) {
        listItemFactories[type] = factory
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> getListItem(type: ViewType): ListItemViewFactory<T>? =
        listItemFactories[type] as? ListItemViewFactory<T>

    fun <T> registerLayout(type: ViewType, factory: LayoutViewFactory<T>) {
        layoutFactories[type] = factory
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> getLayout(type: ViewType): LayoutViewFactory<T>? =
        layoutFactories[type] as? LayoutViewFactory<T>
}
