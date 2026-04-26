package io.rippledown.casecontrol

import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.rippledown.caseview.CaseTableBody
import io.rippledown.caseview.CaseViewHandler
import io.rippledown.caseview.ColumnWidths
import io.rippledown.caseview.HeaderRow
import io.rippledown.constants.caseview.CASEVIEW_CASE_NAME_ID
import io.rippledown.constants.caseview.CASE_VIEW_TABLE
import io.rippledown.interpretation.InterpretationView
import io.rippledown.interpretation.InterpretationViewHandler
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.diff.Diff

interface CaseInspectionHandler : CaseViewHandler, InterpretationViewHandler

@Composable
fun CaseInspection(
    case: ViewableCase,
    diff: Diff? = null,
    ruleConditions: List<String> = emptyList(),
    handler: CaseInspectionHandler,
    modifier: Modifier = Modifier
) {
    val columnWidths = ColumnWidths(case.numberOfColumns)
    CaseInspectionLayout(
        modifier = modifier
            .fillMaxHeight()
            .padding(start = 5.dp)
            // Tag the header + body + interpretation as a single
            // accessibility region so the integration page-object can narrow
            // queries (date cells live in the fixed header, attribute and
            // value cells live in the scrolling body) to one container.
            .semantics { contentDescription = CASE_VIEW_TABLE },
        caseHeader = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = case.name,
                        style = MaterialTheme.typography.subtitle1,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colors.onSurface,
                        textAlign = TextAlign.End,
                        modifier = Modifier
                            .weight(columnWidths.attributeColumnWeight)
                            .padding(end = 12.dp)
                            .testTag("$CASEVIEW_CASE_NAME_ID${case.name}")
                            .semantics {
                                contentDescription = CASEVIEW_CASE_NAME_ID
                            }
                    )
                    Spacer(modifier = Modifier.weight(1f - columnWidths.attributeColumnWeight))
                }
                Spacer(modifier = Modifier.height(16.dp))
                HeaderRow(columnWidths, case.dates)
            }
        },
        caseBody = {
            CaseTableBody(
                viewableCase = case,
                columnWidths = columnWidths,
                attributeMoveListener = handler::swapAttributes
            )
        },
        interpretationContent = {
            InterpretationView(
                interpretation = case.viewableInterpretation,
                diff = diff,
                ruleConditions = ruleConditions,
                handler = handler
            )
        }
    )
}
