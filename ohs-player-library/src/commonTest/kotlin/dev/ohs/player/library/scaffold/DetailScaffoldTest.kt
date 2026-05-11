package dev.ohs.player.library.scaffold

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
import dev.ohs.player.library.registry.registerComponent
import dev.ohs.player.library.renderer.ComponentRenderer
import kotlin.test.Test
import androidx.compose.foundation.text.BasicText as Text

private val SectionA = ViewType("SectionA")
private val SectionB = ViewType("SectionB")
private val SectionC = ViewType("SectionC")

private data object DetailTestConfig

private class LabeledRenderer(private val label: String) : ComponentRenderer<String, DetailTestConfig> {
    @Composable
    override fun Render(item: String, config: DetailTestConfig, onClick: () -> Unit, modifier: Modifier) {
        Text("[$label] $item")
    }
}

@OptIn(ExperimentalTestApi::class)
class DetailScaffoldTest {

    @Test
    fun nullItem_showsNotFound_andSkipsSections() = runComposeUiTest {
        val registry = ViewRegistry().apply {
            registerComponent(SectionA, LabeledRenderer("A"), DetailTestConfig)
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
            registerComponent(SectionA, LabeledRenderer("A"), DetailTestConfig)
            registerComponent(SectionB, LabeledRenderer("B"), DetailTestConfig)
            registerComponent(SectionC, LabeledRenderer("C"), DetailTestConfig)
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
