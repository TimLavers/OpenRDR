@file:OptIn(ExperimentalComposeUiApi::class, ExperimentalComposeUiApi::class)

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.Surface
import androidx.compose.material.Tab
import androidx.compose.material.TabRow
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.rippledown.constants.interpretation.INTERPRETATION_TAB_CHANGES_LABEL
import io.rippledown.constants.interpretation.INTERPRETATION_TAB_CONCLUSIONS_LABEL
import io.rippledown.constants.interpretation.INTERPRETATION_TAB_ORIGINAL_LABEL
import io.rippledown.interpretation.ConclusionsView
import io.rippledown.interpretation.InterpretationView
import io.rippledown.interpretation.InterpretationViewHandler
import io.rippledown.model.diff.Diff
import io.rippledown.model.interpretationview.ViewableInterpretation

interface InterpretationTabsHandler {
    var onStartRule: (selectedDiff: Diff) -> Unit
    var onInterpretationEdited: (text: String) -> Unit
    var isCornerstone: Boolean
}

//val lightBlueColor = Color(100, 149, 237)
val lightBlueColor = Color(70, 130, 180)
@Composable
fun InterpretationTabs(viewableInterpretation: ViewableInterpretation, handler: InterpretationTabsHandler) {
    val titles = listOf(
        INTERPRETATION_TAB_ORIGINAL_LABEL,
        INTERPRETATION_TAB_CONCLUSIONS_LABEL,
        INTERPRETATION_TAB_CHANGES_LABEL
    )

    var tabPage by remember { mutableStateOf(0) }
    val hoverList = remember { mutableStateListOf(false, false, false) }
    val interactionSource = remember { MutableInteractionSource() }

    Column(modifier = Modifier
        .semantics { testTag = "tabs" }
        .fillMaxWidth()
    ) {
        TabRow(selectedTabIndex = tabPage,
            modifier = Modifier.semantics {
                contentDescription = "interpretation_tabs"
            }
        ) {
            titles.forEachIndexed { index, title ->
                val isSelected = tabPage == index
                Tab(
                    selected = isSelected,
                    onClick = { tabPage = index },
                    modifier = Modifier.semantics {
                        contentDescription = "interpretation_tab_$title"
                    }.background(Color.White)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .padding(5.dp)
                            .onPointerEvent(PointerEventType.Enter) {
                                hoverList[index] = true
                            }
                            .onPointerEvent(PointerEventType.Exit) {
                                hoverList[index] = false
                            }
                            .hoverable(
                                interactionSource = interactionSource,
                            )
                    ) {
                        Image(painter = painterForIndex(index), null)
                        Text(
                            text = title,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color.Blue else {
                                if (hoverList[index] == true) lightBlueColor else Color.Black
                            },
                            fontSize = 16.sp,
                        )
                    }


                }
            }
        }
        Tooltip(indexOfHoverPage(hoverList))
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

@Composable
fun Tooltip(index: Int) {
    Surface(
        color = Color.White,
        contentColor = Color.Black,
        modifier = Modifier
            .height(20.dp)
            .padding(horizontal = 40.dp)
    ) {
        if (index != -1) {
            Box(
                contentAlignment = Alignment.Center,
            )
            {
                Text(
                    text = when (index) {
                        0 -> "The report given by the rules in the project. Edit it if not appropriate for this case."
                        1 -> "The comments comprising the report, and the reasons why each comment is present."
                        else -> "The differences between the original report and the one you edited. You can build a rule for each difference."
                    },
                    color = Color.Gray,
                )
            }
        }
    }
}


fun indexOfHoverPage(hoverList: List<Boolean>): Int {
    return hoverList.indexOfFirst { it }
}
