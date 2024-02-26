import androidx.compose.foundation.layout.Column
import androidx.compose.material.Tab
import androidx.compose.material.TabRow
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import io.rippledown.constants.interpretation.INTERPRETATION_TAB_CHANGES
import io.rippledown.constants.interpretation.INTERPRETATION_TAB_CONCLUSIONS
import io.rippledown.constants.interpretation.INTERPRETATION_TAB_ORIGINAL
import io.rippledown.interpretation.InterpretationView
import io.rippledown.interpretation.InterpretationViewHandler
import io.rippledown.main.Handler
import io.rippledown.model.diff.Diff
import io.rippledown.model.interpretationview.ViewableInterpretation

interface InterpretationTabsHandler : Handler {
    var interpretation: ViewableInterpretation
    var onStartRule: (selectedDiff: Diff) -> Unit
    var isCornerstone: Boolean
}

@Composable
fun InterpretationTabs(handler: InterpretationTabsHandler) {
    val titles = listOf("Interpretation", "Conclusions", "Changes")

    var tabPage by remember { mutableStateOf(0) }


    Column {
        TabRow(selectedTabIndex = tabPage,
            modifier = Modifier.semantics {
                contentDescription = "interpretation_tabs"
            }
        ) {
            titles.forEachIndexed { index, title ->
                Tab(
                    text = { Text(title) },
                    selected = tabPage == index,
                    onClick = { tabPage = index }
                )
            }
        }
        when (tabPage) {
            0 -> {
                InterpretationView(handler = object : InterpretationViewHandler {
                    override var text: String = handler.interpretation.latestText()
                    override var onEdited: (text: String) -> Unit = { }
                    override var isCornertone: Boolean = false
                })
            }

            1 -> {/* Content of Tab2 */
            }

            2 -> {/* Content of Tab1 */
            }

        }
    }
}