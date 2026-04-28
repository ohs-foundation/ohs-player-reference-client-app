package dev.ohs.player.reference.app.config

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.ohs.player.library.config.ViewComponent
import dev.ohs.player.generated.TextConfig

class TextFactory<T : Any>(
    override val component: ViewComponent,
    private val textExtractor: (T) -> String?,
    private val config: TextConfig? = null,
    private val onClick: (() -> Unit)? = null
) : ViewComponentFactory<T> {
    @Composable
    override fun Render(state: T) {
        Card(
            modifier = Modifier
                .padding(config?.padding?.dp ?: 8.dp)
                .clickable(enabled = onClick != null) { onClick?.invoke() }
        ) {
            Text(
                text = textExtractor(state) ?: "Unknown",
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}
