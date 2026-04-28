package dev.ohs.player.reference.app.config

import androidx.compose.runtime.Composable
import dev.ohs.player.library.config.ViewComponent

/**
 * T is the generated state class (e.g., PatientHeaderState).
 */
interface ViewComponentFactory<T : Any> {
    val component: ViewComponent

    @Composable
    fun Render(state: T)
}