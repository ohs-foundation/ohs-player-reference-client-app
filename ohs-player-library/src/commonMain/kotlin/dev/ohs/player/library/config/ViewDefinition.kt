package dev.ohs.player.library.config

import kotlinx.serialization.Serializable

@Serializable
data class ViewDefinition(
    val name: String,
    val resource: String,
    val status: String = "active",
    val select: List<SelectBlock> = emptyList()
) {
    fun allColumns(): List<ViewColumn> = select.flatMap { it.column }
}

@Serializable
data class SelectBlock(
    val column: List<ViewColumn> = emptyList()
)

@Serializable
data class ViewColumn(
    val name: String,
    val path: String
)