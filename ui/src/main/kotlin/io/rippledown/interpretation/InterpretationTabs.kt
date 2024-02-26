import androidx.compose.foundation.layout.Column
import androidx.compose.material.Tab
import androidx.compose.material.TabRow
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import io.rippledown.constants.interpretation.INTERPRETATION_TAB_CHANGES
import io.rippledown.constants.interpretation.INTERPRETATION_TAB_CONCLUSIONS
import io.rippledown.constants.interpretation.INTERPRETATION_TAB_ORIGINAL
import io.rippledown.interpretation.InterpretationView
import io.rippledown.interpretation.InterpretationViewHandler
import io.rippledown.main.Handler
import io.rippledown.model.diff.Diff
import io.rippledown.model.interpretationview.ViewableInterpretation

interface InterpretationTabsHandler {
    var interpretation: ViewableInterpretation
    var onStartRule: (selectedDiff: Diff) -> Unit
    var isCornerstone: Boolean
}

@Composable
fun InterpretationTabs(handler: InterpretationTabsHandler) {
    val titles = listOf("Interpretation", "Conclusions", "Changes")

    var tabPage by remember { mutableStateOf(0) }


    Column(modifier = Modifier.semantics { testTag = "tabs" }) {
        TabRow(selectedTabIndex = tabPage,
            modifier = Modifier.semantics {
                contentDescription = "interpretation_tabs"
            }
        ) {
            titles.forEachIndexed { index, title ->
                Tab(
                    text = { Text(title) },
                    selected = tabPage == index,
                    onClick = { tabPage = index },
                    modifier = Modifier.semantics {
                        contentDescription = "interpretation_tab_$title"
                    }
                )
            }
        }
        println("latest text ${handler.interpretation.latestText()}")
        when (tabPage) {
            0 -> {
                InterpretationView(handler = object : InterpretationViewHandler {
                    override var text: String = handler.interpretation.latestText()
                    override var onEdited: (text: String) -> Unit = {
                        println("onEdited in InterpTabs called with text: '$it'")
                    }
                    override var isCornertone: Boolean = false
                })
            }

            1 -> {}
            2 -> {}
        }
    }
}
