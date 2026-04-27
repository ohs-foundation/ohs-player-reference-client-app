package dev.ohs.player.reference.client.library.registry

import kotlin.reflect.KClass

data class ViewTypeKey<T : Any>(val viewType: ViewType, val dataType: KClass<T>)
