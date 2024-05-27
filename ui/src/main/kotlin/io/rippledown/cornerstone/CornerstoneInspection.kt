package io.rippledown.cornerstone

import InterpretationTabs
import InterpretationTabsHandler
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.rippledown.caseview.CaseTable
import io.rippledown.constants.cornerstone.*
import io.rippledown.model.diff.Diff
import io.rippledown.model.rule.CornerstoneStatus

@Composable
fun CornerstoneInspection(status: CornerstoneStatus) {
    if (status.numberOfCornerstones == 0) {
        Text(
            text = NO_CORNERSTONES_TO_REVIEW_MSG,
            modifier = Modifier.padding(10.dp)
                .semantics { contentDescription = NO_CORNERSTONES_TO_REVIEW_ID }
        )
    } else {
        val case = status.cornerstoneToReview!!
        Column(
            verticalArrangement = Arrangement.Top,
            modifier = Modifier
                .fillMaxHeight()
                .padding(start = 5.dp)
                .width(600.dp)
                .border(1.dp, MaterialTheme.colors.primary)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = CORNERSTONE_TITLE,
                    style = MaterialTheme.typography.caption,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    color = Color.DarkGray,
                    textAlign = TextAlign.Start,
                    modifier = Modifier
                        .semantics {
                            contentDescription = CORNERSTONE_ID
                        }
                )
                Spacer(modifier = Modifier.width(20.dp))
                Text(
                    text = case.name,
                    style = MaterialTheme.typography.subtitle1,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    color = MaterialTheme.colors.primary,
                    textAlign = TextAlign.Start,
                    modifier = Modifier
                        .semantics {
                            contentDescription = CORNERSTONE_CASE_NAME_ID
                        }
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            CaseTable(case)
            InterpretationTabs(case.viewableInterpretation, object : InterpretationTabsHandler {
                override fun onStartRule(selectedDiff: Diff) {}
                override var isCornerstone: Boolean = true
                override var onInterpretationEdited: (text: String) -> Unit = {}
            })
        }
    }
}