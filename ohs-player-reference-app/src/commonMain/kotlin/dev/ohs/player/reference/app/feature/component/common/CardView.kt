package dev.ohs.player.reference.app.feature.component.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class CardDslScope {
    internal var headerContent: (@Composable () -> Unit)? = null
    internal var bodyContent: (@Composable () -> Unit)? = null
    internal var footerContent: (@Composable () -> Unit)? = null

    fun header(content: @Composable () -> Unit) {
        headerContent = content
    }

    fun body(content: @Composable () -> Unit) {
        bodyContent = content
    }

    fun footer(content: @Composable () -> Unit) {
        footerContent = content
    }
}

@Composable
fun CardView(
    elevationDp: Float = 2f,
    contentPaddingDp: Float = 16f,
    onClick: (() -> Unit)? = null,
    builder: CardDslScope.() -> Unit,
) {
    val scope = CardDslScope().apply(builder)
    val cardModifier = if (onClick != null) {
        Modifier.fillMaxWidth().clickable(onClick = onClick)
    } else {
        Modifier.fillMaxWidth()
    }
    Card(
        modifier = cardModifier,
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = elevationDp.dp),
    ) {
        Column(
            modifier = Modifier.padding(contentPaddingDp.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            scope.headerContent?.invoke()
            scope.bodyContent?.invoke()
            scope.footerContent?.invoke()
        }
    }
}
