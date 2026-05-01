package dev.ohs.player.library.scaffold

import androidx.compose.foundation.text.BasicText as Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import dev.ohs.player.library.registry.LocalViewRegistry
import dev.ohs.player.library.registry.ViewRegistry
import dev.ohs.player.library.registry.ViewType
import dev.ohs.player.library.registry.registerItem
import dev.ohs.player.library.renderer.Renderer
import kotlin.test.Test

private data object SectionA : ViewType { override val value = "SectionA" }
private data object SectionB : ViewType { override val value = "SectionB" }
private data object SectionC : ViewType { override val value = "SectionC" }

private class LabeledRenderer(private val label: String) : Renderer<String> {
    @Composable
    override fun Render(item: String, onClick: () -> Unit, modifier: Modifier) {
        Text("[$label] $item")
    }
}

@OptIn(ExperimentalTestApi::class)
class DetailScaffoldTest {

    @Test
    fun nullItem_showsNotFound_andSkipsSections() = runComposeUiTest {
        val registry = ViewRegistry().apply {
            registerItem<String>(SectionA, LabeledRenderer("A"))
        }
        setContent {
            CompositionLocalProvider(LocalViewRegistry provides registry) {
                DetailScaffold<String>(item = null) {
                    section(SectionA)
                    notFound { Text("missing") }
                }
            }
        }
        onNodeWithText("missing").assertIsDisplayed()
        // The section renderer would emit "[A] x" if invoked; assert no such node exists.
        onAllNodesWithText("[A] x").assertCountEquals(0)
    }

    @Test
    fun sections_renderInDeclaredOrder() = runComposeUiTest {
        val registry = ViewRegistry().apply {
            registerItem<String>(SectionA, LabeledRenderer("A"))
            registerItem<String>(SectionB, LabeledRenderer("B"))
            registerItem<String>(SectionC, LabeledRenderer("C"))
        }
        setContent {
            CompositionLocalProvider(LocalViewRegistry provides registry) {
                DetailScaffold<String>(item = "x") {
                    section(SectionA)
                    section(SectionB)
                    section(SectionC)
                }
            }
        }
        onNodeWithText("[A] x").assertIsDisplayed()
        onNodeWithText("[B] x").assertIsDisplayed()
        onNodeWithText("[C] x").assertIsDisplayed()
    }
}
