import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Tab
import androidx.compose.material.TabRow
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.unit.dp
import io.rippledown.constants.interpretation.INTERPRETATION_TAB_CHANGES_LABEL
import io.rippledown.constants.interpretation.INTERPRETATION_TAB_CONCLUSIONS_LABEL
import io.rippledown.constants.interpretation.INTERPRETATION_TAB_ORIGINAL_LABEL
import io.rippledown.interpretation.InterpretationView
import io.rippledown.interpretation.InterpretationViewHandler
import io.rippledown.model.diff.Diff
import io.rippledown.model.interpretationview.ViewableInterpretation

interface InterpretationTabsHandler {
    var onStartRule: (selectedDiff: Diff) -> Unit
    var onInterpretationEdited: (text: String) -> Unit
    var isCornerstone: Boolean
}

@Composable
fun InterpretationTabs(viewableInterpretation: ViewableInterpretation, handler: InterpretationTabsHandler) {
    val titles = listOf(
        INTERPRETATION_TAB_ORIGINAL_LABEL,
        INTERPRETATION_TAB_CONCLUSIONS_LABEL,
        INTERPRETATION_TAB_CHANGES_LABEL
    )

    var tabPage by remember { mutableStateOf(0) }

    Column(modifier = Modifier.semantics { testTag = "tabs" }) {
        TabRow(selectedTabIndex = tabPage,
            modifier = Modifier.semantics {
                contentDescription = "interpretation_tabs"
            }
                .padding(10.dp)

        ) {
            titles.forEachIndexed { index, title ->
                Tab(
                    text = { Text(title) },
                    selected = tabPage == index,
                    onClick = { tabPage = index },
                    modifier = Modifier.semantics {
                        contentDescription = "interpretation_tab_$title"
                    }
                        .background(Color.Gray)
                )
            }
        }
        when (tabPage) {
            0 -> {
                InterpretationView(
                    text = viewableInterpretation.latestText(),
                    handler = object : InterpretationViewHandler {
                        override var onEdited: (text: String) -> Unit = { editedText ->
                            println("Updating interp with editedText = ${editedText}")
                            handler.onInterpretationEdited(editedText)
                        }
                        override var isCornertone: Boolean = false
                    })
            }

            1 -> {}
            2 -> {}
        }
    }
}
