package dev.ohs.player.reference.client.library.registry

import androidx.compose.runtime.Composable

fun interface ListItemViewFactory<T> {
    @Composable
    fun Content(data: T, onClick: () -> Unit)
}
