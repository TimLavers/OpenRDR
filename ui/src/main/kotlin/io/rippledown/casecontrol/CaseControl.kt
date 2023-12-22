package io.rippledown.casecontrol

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.rippledown.constants.caseview.CASES
import io.rippledown.constants.caseview.NUMBER_OF_CASES_ID
import io.rippledown.main.Handler
import io.rippledown.model.CaseId
import io.rippledown.model.caseview.ViewableCase

interface CaseControlHandler : Handler {
    var caseIds: List<CaseId>
    var setRuleInProgress: (_: Boolean) -> Unit
}

@Composable
@Preview
fun CaseControl(handler: CaseControlHandler) {
    var currentCase: ViewableCase? by remember { mutableStateOf(null) }
    var showSelector by remember { mutableStateOf(true) }
    var currentCaseId : Long? by remember { mutableStateOf(null) }

    println("CaseControl: ${handler.caseIds.size}")

    LaunchedEffect(Unit) {
        if (currentCase == null && handler.caseIds.isNotEmpty()) {
            currentCaseId = handler.caseIds[0].id!!
        }
    }

    LaunchedEffect(currentCaseId) {
        currentCase = handler.api.getCase(currentCaseId!!)
    }

    fun selectFirstCase() {
        val names = handler.caseIds.map { it.name }
        val currentCaseNullOrNotAvailable = currentCase == null || !names.contains(currentCase?.name)
        if (currentCaseNullOrNotAvailable && names.isNotEmpty()) {
            val firstCaseId = handler.caseIds[0]
            currentCaseId = firstCaseId.id!!
        }
    }

    selectFirstCase()

    Column(
        modifier = Modifier
            .padding(10.dp)
            .testTag("caseControl"),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = "$CASES ${handler.caseIds.size}",
            style = MaterialTheme.typography.subtitle1,
            textAlign = TextAlign.Start,
            modifier = Modifier
                .testTag(NUMBER_OF_CASES_ID)
        )

        CaseSelector(object : CaseSelectorHandler, CaseControlHandler by handler {
            override var selectCase = { id: Long -> currentCaseId = id }
        })
    }
}