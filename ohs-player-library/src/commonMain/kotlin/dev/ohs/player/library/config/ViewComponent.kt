package dev.ohs.player.library.config


import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlin.jvm.JvmInline

@Serializable
@JvmInline
value class ViewType(val value: String)

/**
 * Represents a generic UI component as defined in the IG.
 *
 */
@Serializable
data class ViewComponent(
  val viewId: String,
  val type: ViewType, // Matches ViewTypeCS codes
  val visibleWhen: JsonElement? = null, // Dynamic visibility logic
  val config: JsonElement? = null // To be decoded into Generated ViewConfigs
)