package dev.ohs.player.library.scaffold

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import dev.ohs.player.library.registry.LocalViewRegistry
import dev.ohs.player.library.registry.ViewRegistry
import dev.ohs.player.library.registry.ViewType
import dev.ohs.player.library.registry.registerItem
import dev.ohs.player.library.registry.registerLayout
import dev.ohs.player.library.renderer.LayoutRenderer
import dev.ohs.player.library.renderer.Renderer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertTrue
import androidx.compose.foundation.text.BasicText as Text

private data object TextItemViewType : ViewType {
    override val value = "TextItem"
}
private data object PlainListViewType : ViewType {
    override val value = "PlainList"
}

private class TextRenderer : Renderer<String> {
    @Composable
    override fun Render(item: String, onClick: () -> Unit, modifier: Modifier) {
        Text(text = item, modifier = modifier.clickable { onClick() })
    }
}

private class RecordingLayout : LayoutRenderer<String> {
    var renderInvocations = 0
        private set

    @Composable
    override fun Render(
        items: List<String>,
        itemRenderer: Renderer<String>,
        key: (String) -> Any,
        onItemClick: (String) -> Unit,
        modifier: Modifier,
    ) {
        renderInvocations++
        Column {
            items.forEach { item ->
                itemRenderer.Render(item = item, onClick = { onItemClick(item) }, modifier = Modifier)
            }
        }
    }
}

@OptIn(ExperimentalTestApi::class)
class ListScaffoldTest {

    @Test
    fun emptyList_showsEmptyState_andDoesNotInvokeLayout() = runComposeUiTest {
        val registry = ViewRegistry().apply {
            registerItem<String>(TextItemViewType, TextRenderer())
        }
        val layout = RecordingLayout()

        setContent {
            CompositionLocalProvider(LocalViewRegistry provides registry) {
                ListScaffold<String>(
                    items = emptyList(),
                    onItemClick = {},
                    key = { it },
                ) {
                    item(TextItemViewType)
                    layout(layout)
                    emptyState { Text("nothing here") }
                }
            }
        }

        onNodeWithText("nothing here").assertIsDisplayed()
        assertEquals(0, layout.renderInvocations)
    }

    @Test
    fun rendersItems_andForwardsClicksFromRegistry() = runComposeUiTest {
        var clicked: String? = null
        val registry = ViewRegistry().apply {
            registerItem<String>(TextItemViewType, TextRenderer())
            registerLayout<String>(PlainListViewType, RecordingLayout())
        }

        setContent {
            CompositionLocalProvider(LocalViewRegistry provides registry) {
                ListScaffold<String>(
                    items = listOf("alpha", "beta", "gamma"),
                    onItemClick = { clicked = it },
                    key = { it },
                ) {
                    item(TextItemViewType)
                    layout(PlainListViewType)
                }
            }
        }

        onNodeWithText("alpha").assertIsDisplayed()
        onNodeWithText("beta").assertIsDisplayed()
        onNodeWithText("gamma").assertIsDisplayed()

        onNodeWithText("beta").performClick()
        assertEquals("beta", clicked)
    }

    @Test
    fun omittingLayout_fallsBackToVerticalListRenderer() = runComposeUiTest {
        val registry = ViewRegistry().apply {
            registerItem<String>(TextItemViewType, TextRenderer())
        }

        setContent {
            CompositionLocalProvider(LocalViewRegistry provides registry) {
                ListScaffold<String>(
                    items = listOf("one", "two"),
                    onItemClick = {},
                    key = { it },
                ) {
                    item(TextItemViewType)
                }
            }
        }

        onNodeWithText("one").assertIsDisplayed()
        onNodeWithText("two").assertIsDisplayed()
    }

    @Test
    fun unregisteredViewType_throwsWithDescriptiveMessage() = runComposeUiTest {
        val thrown = assertFails {
            setContent {
                ListScaffold<String>(items = listOf("x"), onItemClick = {}, key = { it }) {
                    item(TextItemViewType)
                }
            }
        }
        val msg = thrown.message.orEmpty()
        assertTrue(msg.contains("String"), "Message should mention data type: $msg")
        assertTrue(msg.contains(TextItemViewType.value), "Message should mention view type value: $msg")
    }
}
