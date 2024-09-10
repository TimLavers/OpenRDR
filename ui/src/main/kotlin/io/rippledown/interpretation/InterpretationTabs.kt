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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.rippledown.constants.interpretation.INTERPRETATION_TABS
import io.rippledown.constants.interpretation.INTERPRETATION_TAB_CONCLUSIONS_LABEL
import io.rippledown.constants.interpretation.INTERPRETATION_TAB_ORIGINAL_LABEL
import io.rippledown.constants.interpretation.INTERPRETATION_TAB_PREFIX
import io.rippledown.interpretation.ConclusionsView
import io.rippledown.interpretation.InterpretationView
import io.rippledown.interpretation.ToolTipForIconAndLabel
import io.rippledown.model.interpretationview.ViewableInterpretation


@Composable
fun InterpretationTabs(viewableInterpretation: ViewableInterpretation) {
    val titles = buildList {
        add(INTERPRETATION_TAB_ORIGINAL_LABEL)
        add(INTERPRETATION_TAB_CONCLUSIONS_LABEL)
    }

    var tabPage by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        TabRow(selectedTabIndex = tabPage,
            modifier = Modifier.semantics {
                contentDescription = INTERPRETATION_TABS
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
                    },
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
            0 -> InterpretationView(viewableInterpretation)
            1 -> ConclusionsView(viewableInterpretation)
        }
    }
}

@Composable
fun painterForIndex(index: Int) =
    when (index) {
        0 -> painterResource("layout_24.png")
        else -> painterResource("paragraph_24.png")
    }

fun toolTipTextForIndex(index: Int) =
    when (index) {
        0 -> "The report given by the rules in the project."
        else -> "The comments comprising the report\nand the reasons why each comment is present."
    }