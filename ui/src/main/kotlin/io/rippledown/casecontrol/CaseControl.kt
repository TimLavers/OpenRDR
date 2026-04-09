package io.rippledown.casecontrol

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.rippledown.constants.cornerstone.NO_CORNERSTONES_TO_REVIEW_ID
import io.rippledown.constants.cornerstone.NO_CORNERSTONES_TO_REVIEW_MSG
import io.rippledown.cornerstone.CornerstoneInspection
import io.rippledown.decoration.ItalicGrey
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
        } else if (cornerstoneStatus != null) {
            Text(
                text = NO_CORNERSTONES_TO_REVIEW_MSG,
                style = ItalicGrey,
                modifier = Modifier.semantics {
                    contentDescription = NO_CORNERSTONES_TO_REVIEW_ID
                }
            )
        }
    }
}