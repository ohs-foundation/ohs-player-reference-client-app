package dev.ohs.player.library.renderer

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

interface ComponentRenderer<T> {
    @Composable
    fun Render(item: T, onClick: () -> Unit = {}, modifier: Modifier = Modifier)
}
