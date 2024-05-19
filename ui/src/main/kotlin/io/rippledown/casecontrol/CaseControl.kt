package io.rippledown.casecontrol

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.rippledown.constants.interpretation.DEBOUNCE_WAIT_PERIOD_MILLIS
import io.rippledown.main.Handler
import io.rippledown.model.Attribute
import io.rippledown.model.CasesInfo
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.condition.Condition
import io.rippledown.rule.RuleMaker
import io.rippledown.rule.RuleMakerHandler
import kotlinx.coroutines.delay

interface CaseControlHandler : Handler, CaseInspectionHandler {
    var setRuleInProgress: (_: Boolean) -> Unit
    var getCase: (caseId: Long) -> ViewableCase?
    suspend fun saveCase(case: ViewableCase): ViewableCase
    suspend fun conditionHintsForCase(caseId: Long): List<Condition>
}

@Composable
@Preview
fun CaseControl(ruleInProgress: Boolean, casesInfo: CasesInfo, handler: CaseControlHandler) {
    var currentCase: ViewableCase? by remember { mutableStateOf(null) }
    var showSelector by remember { mutableStateOf(true) }
    var currentCaseId: Long? by remember { mutableStateOf(null) }
    var verifiedText: String? by remember { mutableStateOf(null) }
    var indexOfSelectedDiff: Int by remember { mutableStateOf(-1) }
    var showRuleMaker by remember { mutableStateOf(ruleInProgress) }
    var conditionHintsForCase by remember { mutableStateOf(listOf<Condition>()) }

    LaunchedEffect(casesInfo, currentCaseId) {
        if (casesInfo.caseIds.isNotEmpty()) {
            if (currentCaseId == null || currentCaseId !in casesInfo.caseIds.map { it.id }) {
                //No initial case, or it's now been deleted
                currentCaseId = casesInfo.caseIds[0].id!!
            }
            currentCase = handler.getCase(currentCaseId!!)
            conditionHintsForCase = handler.conditionHintsForCase(currentCaseId!!)
        }
    }
    LaunchedEffect(verifiedText, indexOfSelectedDiff) {
        if (verifiedText != null || indexOfSelectedDiff != -1) {
            delay(DEBOUNCE_WAIT_PERIOD_MILLIS)
            currentCase = handler.saveCase(currentCase!!)
        }
    }

    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    )
    {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            CaseSelectorHeader(casesInfo.caseIds.size)
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
                override var onStartRule: (indexOfSelectedDiff: Int) -> Unit = {
                    indexOfSelectedDiff = it
                    val updatedDiffList = currentCase!!.viewableInterpretation.diffList.apply { selected = it }
                    val updatedCase = currentCase!!
                        .copy(
                            viewableInterpretation = currentCase!!.viewableInterpretation
                                .copy(diffList = updatedDiffList)
                        )
                    currentCase = updatedCase
                    showRuleMaker = true
                    handler.setRuleInProgress(true)
                }
                override var onInterpretationEdited: (text: String) -> Unit = {
                    verifiedText = it
                    currentCase = currentCase!!.copy(
                        viewableInterpretation = currentCase!!.viewableInterpretation
                            .copy(verifiedText = verifiedText)
                    )
                }
                override var isCornerstone: Boolean = false
                override var caseEdited: () -> Unit = {}
                override fun swapAttributes(moved: Attribute, target: Attribute) {
                    handler.swapAttributes(moved, target)
                }
            })

            if (showRuleMaker) {
                println("Showing RuleMaker")
                RuleMaker(conditionHintsForCase, object : RuleMakerHandler, Handler by handler {
                    override var onDone = { conditions: List<Condition> ->
                        showRuleMaker = false
                    }

                    override var onCancel = {
                        showRuleMaker = false
                    }
                })
            }
        }
    }
}