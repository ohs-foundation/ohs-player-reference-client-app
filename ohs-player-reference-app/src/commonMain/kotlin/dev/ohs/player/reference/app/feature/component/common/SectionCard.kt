package dev.ohs.player.reference.app.feature.component.common

import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun SectionCard(title: String, content: @Composable () -> Unit) {
    CardView {
        header {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            HorizontalDivider()
        }
        body { content() }
    }
}
