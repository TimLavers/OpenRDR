package io.rippledown.casecontrol

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.rippledown.constants.cornerstone.NO_CORNERSTONES_TO_REVIEW_MSG
import io.rippledown.cornerstone.CornerstonePager
import io.rippledown.cornerstone.CornerstonePagerHandler
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.rule.CornerstoneStatus

interface CaseControlHandler : CaseInspectionHandler, CornerstonePagerHandler {
    var setRightInfoMessage: (message: String) -> Unit
}

@Composable
fun CaseControl(
    currentCase: ViewableCase?,
    cornerstoneStatus: CornerstoneStatus? = null,
    handler: CaseControlHandler,
    modifier: Modifier = Modifier
) {
    val ruleInProgress = cornerstoneStatus != null

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
        if (ruleInProgress) {
            if (cornerstoneStatus.cornerstoneToReview == null) {
                handler.setRightInfoMessage(NO_CORNERSTONES_TO_REVIEW_MSG)
            } else {
                handler.setRightInfoMessage("")
                CornerstonePager(cornerstoneStatus, handler)
            }
        }
    }
}