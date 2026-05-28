package io.rippledown.casecontrol

import androidx.compose.foundation.HorizontalScrollbar
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
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
import kotlinx.coroutines.flow.distinctUntilChanged

interface CaseInspectionHandler : CaseViewHandler, InterpretationViewHandler

@Composable
fun CaseInspection(
    case: ViewableCase,
    diff: Diff? = null,
    ruleConditions: List<String> = emptyList(),
    handler: CaseInspectionHandler,
    modifier: Modifier = Modifier,
    filter: String = ""
) {
    val columnWidths = ColumnWidths(case.numberOfColumns)
    // Header dates row and attribute body share a single horizontal scroll
    // state so the date and value columns stay aligned when scrolled. Only
    // the dates and values scroll horizontally — the attribute, reference
    // range and units columns stay fixed.
    val hScrollState = rememberScrollState()
    val hScrollbarAdapter = rememberScrollbarAdapter(hScrollState)
    val multiEpisode = case.dates.size > 1
    // When a case with multiple episodes is shown, auto-scroll the table
    // fully right so the most recent episode is visible. We collect
    // maxValue (rather than just taking the first non-zero value) because
    // the slot width can shrink after the initial layout — e.g. when a
    // rule session starts and the cornerstone panel appears next to this
    // one — which grows maxValue. We re-anchor to the new max only while
    // the user is still pinned at the previous max, so manual scrolls are
    // never overridden.
    LaunchedEffect(case) {
        if (multiEpisode) {
            var lastMax = 0
            snapshotFlow { hScrollState.maxValue }
                .distinctUntilChanged()
                .collect { newMax ->
                    if (newMax > 0 && (lastMax == 0 || hScrollState.value == lastMax)) {
                        hScrollState.scrollTo(newMax)
                    }
                    lastMax = newMax
                }
        }
    }
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
            // Match the horizontal padding applied inside CaseTableBody so the
            // case name and the date header line up with the attribute column.
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 5.dp)) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = case.name,
                        style = MaterialTheme.typography.subtitle1,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colors.onSurface,
                        textAlign = TextAlign.Start,
                        modifier = Modifier
                            .weight(columnWidths.attributeColumnWeight)
                            .padding(end = 12.dp)
                            .testTag("$CASEVIEW_CASE_NAME_ID${case.name}")
                            .semantics {
                                // Embed the case name in the contentDescription so the
                                // Java accessibility bridge exposes it. From Compose 1.11
                                // the bridge uses contentDescription as the accessible
                                // name on a Text node, overriding the rendered text.
                                contentDescription = "$CASEVIEW_CASE_NAME_ID${case.name}"
                            }
                    )
                    Spacer(modifier = Modifier.weight(1f - columnWidths.attributeColumnWeight))
                }
                Spacer(modifier = Modifier.height(16.dp))
                HeaderRow(
                    columnWidths = columnWidths,
                    dates = case.dates,
                    hScrollState = hScrollState,
                )
            }
        },
        caseBody = {
            CaseTableBody(
                viewableCase = case,
                columnWidths = columnWidths,
                attributeMoveListener = handler::swapAttributes,
                hScrollState = hScrollState,
                filter = filter,
            )
        },
        interpretationContent = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (multiEpisode) {
                    // Placed above the interpretation panel (i.e. directly
                    // beneath the case body) so the scrollbar stays visible
                    // even when the body content is long enough to scroll
                    // vertically.
                    HorizontalScrollbar(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp),
                        adapter = hScrollbarAdapter
                    )
                }
                InterpretationView(
                    interpretation = case.viewableInterpretation,
                    diff = diff,
                    ruleConditions = ruleConditions,
                    handler = handler
                )
            }
        }
    )
}
