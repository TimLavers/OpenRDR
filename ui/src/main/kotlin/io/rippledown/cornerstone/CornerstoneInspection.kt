package io.rippledown.cornerstone

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.OutlinedCard
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.rippledown.caseview.CaseTableBody
import io.rippledown.caseview.ColumnWidths
import io.rippledown.caseview.HeaderRow
import io.rippledown.constants.cornerstone.CORNERSTONE_CASE_NAME_ID
import io.rippledown.constants.cornerstone.CORNERSTONE_ID
import io.rippledown.constants.cornerstone.CORNERSTONE_TITLE
import io.rippledown.constants.interpretation.COMMENTS_TOGGLE_FOR_CORNERSTONE
import io.rippledown.constants.interpretation.CORNERSTONE_COMMENT_ID_PREFIX
import io.rippledown.decoration.ItalicGrey
import io.rippledown.interpretation.DerivedValuesPanel
import io.rippledown.interpretation.ReadonlyInterpretationView
import io.rippledown.interpretation.ReadonlyInterpretationViewHandler
import io.rippledown.model.caseview.ViewableCase
import kotlinx.coroutines.flow.first

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CornerstoneInspection(case: ViewableCase, index: Int = 0, total: Int = 0, filter: String = "") {
    val columnWidths = ColumnWidths(case.numberOfColumns)
    val hScrollState = rememberScrollState()
    val hScrollbarAdapter = rememberScrollbarAdapter(hScrollState)
    val multiEpisode = case.dates.size > 1
    var commentsExpanded by remember(case) { mutableStateOf(true) }
    LaunchedEffect(case) {
        if (multiEpisode) {
            snapshotFlow { hScrollState.maxValue }
                .first { it > 0 }
                .let { hScrollState.scrollTo(it) }
        }
    }
    io.rippledown.casecontrol.CaseInspectionLayout(
        modifier = Modifier
            .fillMaxHeight()
            .padding(start = 5.dp)
            .width(500.dp),
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
                            .alignByBaseline()
                            .weight(columnWidths.attributeColumnWeight)
                            .padding(end = 12.dp)
                            .semantics {
                                contentDescription = CORNERSTONE_CASE_NAME_ID
                            }
                    )
                    val cornerstoneLabel =
                        if (total > 0) "$CORNERSTONE_TITLE ${index + 1} of $total" else CORNERSTONE_TITLE
                    Text(
                        text = cornerstoneLabel,
                        style = ItalicGrey,
                        textAlign = TextAlign.Start,
                        modifier = Modifier
                            .alignByBaseline()
                            .weight(1f - columnWidths.attributeColumnWeight)
                            .semantics {
                                contentDescription = CORNERSTONE_ID
                            }
                    )
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
                hScrollState = hScrollState,
                filter = filter,
            )
        },
        interpretationContent = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (multiEpisode) {
                    HorizontalScrollbar(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp),
                        adapter = hScrollbarAdapter
                    )
                }
                DerivedValuesPanel(
                    derivedValues = case.derivedValues(),
                    columnWidths = columnWidths
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                        .padding(top = 4.dp)
                        .clickable { commentsExpanded = !commentsExpanded }
                        .semantics { contentDescription = COMMENTS_TOGGLE_FOR_CORNERSTONE }
                ) {
                    Icon(
                        imageVector = if (commentsExpanded) Icons.Filled.KeyboardArrowDown else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = Color.DarkGray,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Comments",
                        color = Color.DarkGray,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
                if (commentsExpanded) {
                    OutlinedCard(
                        modifier = Modifier.padding(top = 4.dp, bottom = 10.dp),
                        colors = androidx.compose.material3.CardDefaults.outlinedCardColors(
                            containerColor = androidx.compose.ui.graphics.Color.White
                        )
                    ) {
                        ReadonlyInterpretationView(
                            case.viewableInterpretation,
                            idPrefix = CORNERSTONE_COMMENT_ID_PREFIX,
                            modifier = Modifier.fillMaxWidth(),
                            handler = object : ReadonlyInterpretationViewHandler {}
                        )
                    }
                }
            }
        }
    )
}
