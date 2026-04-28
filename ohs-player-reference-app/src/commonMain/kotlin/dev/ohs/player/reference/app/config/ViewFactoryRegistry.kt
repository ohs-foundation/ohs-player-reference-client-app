package dev.ohs.player.reference.app.config

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import dev.ohs.player.library.config.ViewComponent
import dev.ohs.player.library.config.ViewType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement

class ViewFactoryRegistry(private val json: Json = Json { ignoreUnknownKeys = true }) {

    // We store a lambda that handles decoding + rendering, now including an optional onClick handler
    private val renderers = mutableMapOf<String, @Composable (ViewComponent, JsonElement, (() -> Unit)?) -> Unit>()
    /**
     * Register a factory for a specific ViewType.
     * [reified T] it allows us to decode the JSON element to the specific generated state class.
     */
    internal inline fun <reified T : Any> register(
      type: ViewType,
      crossinline factoryProvider: (ViewComponent, (() -> Unit)?) -> ViewComponentFactory<T>
    ) {
        renderers[type.value] = @Composable { component, data, onClick ->
            val state = json.decodeFromJsonElement<T>(data)
            factoryProvider(component, onClick).Render(state)
        }
    }

    @Composable
    fun RenderComponent(component: ViewComponent, data: JsonElement, onClick: (() -> Unit)? = null) {
        val renderer = renderers[component.type.value]
        if (renderer != null) {
            renderer(component, data, onClick)
        } else {
            // Fallback for missing factories
            Text("No factory registered for ${component.type.value}")
        }
    }
}