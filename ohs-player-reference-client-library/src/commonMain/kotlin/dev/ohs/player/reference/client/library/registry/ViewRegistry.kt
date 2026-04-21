package dev.ohs.player.reference.client.library.registry

class ViewRegistry {
    private val listItemFactories = mutableMapOf<ViewType, ListItemViewFactory<*>>()

    fun <T> registerListItem(type: ViewType, factory: ListItemViewFactory<T>) {
        listItemFactories[type] = factory
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> getListItem(type: ViewType): ListItemViewFactory<T>? =
        listItemFactories[type] as? ListItemViewFactory<T>
}
