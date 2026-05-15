package io.rippledown.casecontrol

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.*
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
    // The case-view filter is owned here so that a single string applies
    // uniformly to the current case AND the cornerstone case shown beside
    // it, and survives across case-selection and cornerstone changes (the
    // call site of CaseControl in OpenRDRUI is stable, so `remember` is
    // retained when `currentCase` or `cornerstoneStatus` parameters change).
    var filter by remember { mutableStateOf("") }
    Column(modifier = modifier.padding(8.dp)) {
        CaseViewFilterField(
            value = filter,
            onValueChange = { filter = it },
            onClear = { filter = "" }
        )
        Row {
            if (currentCase != null) {
                CaseInspection(
                    currentCase,
                    cornerstoneStatus?.diff,
                    cornerstoneStatus?.ruleConditions ?: emptyList(),
                    handler,
                    modifier = Modifier.weight(1f),
                    filter = filter
                )
            }
            val cornerstoneToReview = cornerstoneStatus?.cornerstoneToReview
            if (cornerstoneToReview != null) {
                CornerstoneInspection(
                    cornerstoneToReview,
                    index = cornerstoneStatus.indexOfCornerstoneToReview,
                    total = cornerstoneStatus.numberOfCornerstones,
                    filter = filter
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
}