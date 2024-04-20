package io.rippledown.casecontrol

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign.Companion
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.rippledown.constants.caseview.CASES
import io.rippledown.constants.caseview.NUMBER_OF_CASES_ID
import io.rippledown.main.Handler
import io.rippledown.model.Attribute
import io.rippledown.model.CasesInfo
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.diff.Diff
import io.rippledown.model.interpretationview.ViewableInterpretation

interface CaseControlHandler : Handler, CaseInspectionHandler {
    var setRuleInProgress: (_: Boolean) -> Unit
    var getCase: (caseId: Long) -> ViewableCase?
    var saveCase: (case: ViewableCase) -> Unit
}

@Composable
@Preview
fun CaseControl(casesInfo: CasesInfo, handler: CaseControlHandler) {
    var currentCase: ViewableCase? by remember { mutableStateOf(null) }
    var showSelector by remember { mutableStateOf(true) }
    var currentCaseId: Long? by remember { mutableStateOf(null) }

    LaunchedEffect(casesInfo, currentCaseId) {
        if (casesInfo.caseIds.isNotEmpty()) {
            if (currentCaseId == null || currentCaseId !in casesInfo.caseIds.map { it.id }) {
                //No initial case, or it's now been deleted
                currentCaseId = casesInfo.caseIds[0].id!!
            }
            currentCase = handler.getCase(currentCaseId!!)
        }
    }

    Row {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.width(150.dp)
            )
            {
                Text(
                    text = CASES,
                    textAlign = Companion.Left,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    color = MaterialTheme.colors.primary,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = casesInfo.count.toString(),
                    textAlign = Companion.Right,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier.weight(1f).padding(end = 10.dp)
                        .testTag(NUMBER_OF_CASES_ID)
                        .semantics { contentDescription = NUMBER_OF_CASES_ID }

                )
            }

            CaseSelector(casesInfo.caseIds, object : CaseSelectorHandler, Handler by handler {
                override var selectCase = { id: Long ->
                    currentCaseId = id
                }
            })
        }

        if (currentCase != null) {
            CaseInspection(currentCase!!, object : CaseInspectionHandler, Handler by handler {
                override var updateCase = { id: Long ->
                    currentCaseId = id
                }
                override var ruleSessionInProgress: (Boolean) -> Unit = {
                    handler.setRuleInProgress(it)
                }
                override var onStartRule: (selectedDiff: Diff) -> Unit = {}
                override var onInterpretationEdited: (text: String) -> Unit = {
                    //create a new instance of the case with the updated verified text to trigger a redraw
                    val updated = ViewableCase(
                        case = currentCase!!.case,
                        viewableInterpretation = ViewableInterpretation(currentCase!!.case.interpretation).apply {
                            verifiedText = it
                        },
                        viewProperties = currentCase!!.viewProperties
                    )
                    handler.saveCase(updated)
                    currentCase = updated
                }
                override var isCornerstone: Boolean = false
                override var caseEdited: () -> Unit = {}
                override fun swapAttributes(moved: Attribute, target: Attribute) {
                    handler.swapAttributes(moved, target)
                }
            })
        }
    }
}