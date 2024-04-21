@file:OptIn(ExperimentalComposeUiApi::class, ExperimentalComposeUiApi::class)

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.*
import androidx.compose.material.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.unit.dp
import io.rippledown.constants.interpretation.INTERPRETATION_TAB_CHANGES_LABEL
import io.rippledown.constants.interpretation.INTERPRETATION_TAB_CONCLUSIONS_LABEL
import io.rippledown.constants.interpretation.INTERPRETATION_TAB_ORIGINAL_LABEL
import io.rippledown.constants.interpretation.INTERPRETATION_TAB_PREFIX
import io.rippledown.interpretation.ConclusionsView
import io.rippledown.interpretation.InterpretationView
import io.rippledown.interpretation.InterpretationViewHandler
import io.rippledown.interpretation.ToolTipForIconAndLabel
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

    Column(modifier = Modifier
        .semantics { testTag = "tabs" }
        .fillMaxWidth()
    ) {
        TabRow(selectedTabIndex = tabPage,
            modifier = Modifier.semantics {
                contentDescription = "interpretation_tabs"
            },
            indicator = @Composable { tabPositions: List<TabPosition> ->
                TabRowDefaults.Indicator(
                    Modifier.tabIndicatorOffset(tabPositions[tabPage]),
                    color = MaterialTheme.colors.primary,
                    height = 2.dp // Set height
                )
            }
        ) {
            titles.forEachIndexed { index, title ->
                val isSelected = tabPage == index
                Tab(
                    selected = isSelected,
                    onClick = {
                        tabPage = index
                    }, // Do nothing, we handle this in the ToolTipForIconAndLabel
                    modifier = Modifier.semantics {
                        contentDescription = "$INTERPRETATION_TAB_PREFIX$title"
                    }.background(Color.White)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start,
                    ) {
                        ToolTipForIconAndLabel(
                            toolTipText = toolTipTextForIndex(index),
                            labelText = title,
                            iconContentDescription = "icon_content_description",
                            isSelected = isSelected,
                            icon = painterForIndex(index),
                            onClick = {
                                tabPage = index
                            }
                        )
                    }
                }
            }
        }
        when (tabPage) {
            0 -> {
                InterpretationView(
                    text = viewableInterpretation.latestText(),
                    handler = object : InterpretationViewHandler {
                        override var onEdited: (text: String) -> Unit = { editedText ->
                            handler.onInterpretationEdited(editedText)
                        }
                        override var isCornertone: Boolean = false
                    })
            }

            1 -> {
                ConclusionsView(viewableInterpretation)
            }

            2 -> {}
        }
    }
}

@Composable
fun painterForIndex(index: Int): Painter {
    return when (index) {
        0 -> painterResource("write_24.png")
        1 -> painterResource("paragraph_24.png")
        else -> painterResource("plus-minus_24.png")
    }
}

fun toolTipTextForIndex(index: Int): String {
    return when (index) {
        0 -> "The report given by the rules in the project.\nEdit it if not appropriate for this case."
        1 -> "The comments comprising the report\nand the reasons why each comment is present."
        else -> "The differences between the original report and the one you edited.\nYou can build a rule for each difference."
    }
}