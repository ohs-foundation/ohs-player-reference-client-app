package dev.ohs.player.reference.app.ui

sealed class Screen {
    data object HouseholdList : Screen()
    data class HouseholdProfile(val groupId: String) : Screen()
}
