package dev.ohs.player.reference.app.feature.component.common.viewtypes

import dev.ohs.player.library.registry.ViewType

// Item view styles
data object CardViewType : ViewType { override val value = "Card" }
data object RowViewType : ViewType { override val value = "Row" }

// Layout view styles
data object VerticalListViewType : ViewType { override val value = "VerticalList" }
data object HorizontalListViewType : ViewType { override val value = "HorizontalList" }
data object GridViewType : ViewType { override val value = "Grid" }
