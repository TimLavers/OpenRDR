package io.rippledown.casecontrol

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.rippledown.cornerstone.CornerstoneInspection
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.rule.CornerstoneStatus

interface CaseControlHandler : CaseInspectionHandler {
}

@Composable
fun CaseControl(
    currentCase: ViewableCase?,
    cornerstoneStatus: CornerstoneStatus? = null,
    handler: CaseControlHandler,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .padding(8.dp)
    )
    {
        if (currentCase != null) {
            CaseInspection(
                currentCase,
                cornerstoneStatus?.diff,
                cornerstoneStatus?.ruleConditions ?: emptyList(),
                handler
            )
        }
        val cornerstoneToReview = cornerstoneStatus?.cornerstoneToReview
        if (cornerstoneToReview != null) {
            CornerstoneInspection(
                cornerstoneToReview,
                index = cornerstoneStatus.indexOfCornerstoneToReview,
                total = cornerstoneStatus.numberOfCornerstones
            )
        }
    }
}