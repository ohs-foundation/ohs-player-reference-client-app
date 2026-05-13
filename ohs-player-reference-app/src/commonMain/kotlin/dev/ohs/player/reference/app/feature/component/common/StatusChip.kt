package dev.ohs.player.reference.app.feature.component.common

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

@Composable
fun StatusChip(isActive: Boolean) {
    val color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
    Chip(
        label = if (isActive) "Active" else "Inactive",
        containerColor = color.copy(alpha = 0.12f),
        contentColor = color,
    )
}
