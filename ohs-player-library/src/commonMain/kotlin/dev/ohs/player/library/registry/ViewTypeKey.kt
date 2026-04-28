package dev.ohs.player.library.registry

import kotlin.reflect.KClass

data class ViewTypeKey<T : Any>(val viewType: ViewType, val dataType: KClass<T>)
