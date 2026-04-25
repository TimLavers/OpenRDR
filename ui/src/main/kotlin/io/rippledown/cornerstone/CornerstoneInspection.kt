package io.rippledown.cornerstone

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material3.OutlinedCard
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.rippledown.caseview.CaseTable
import io.rippledown.caseview.ColumnWidths
import io.rippledown.constants.cornerstone.CORNERSTONE_CASE_NAME_ID
import io.rippledown.constants.cornerstone.CORNERSTONE_ID
import io.rippledown.constants.cornerstone.CORNERSTONE_TITLE
import io.rippledown.decoration.ItalicGrey
import io.rippledown.interpretation.ReadonlyInterpretationView
import io.rippledown.interpretation.ReadonlyInterpretationViewHandler
import io.rippledown.model.caseview.ViewableCase

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CornerstoneInspection(case: ViewableCase, index: Int = 0, total: Int = 0) {
    Column(
        verticalArrangement = Arrangement.Top,
        modifier = Modifier
            .fillMaxHeight()
            .padding(start = 5.dp)
            .width(500.dp)
    ) {
        val columnWidths = ColumnWidths(case.numberOfColumns)
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
            val cornerstoneLabel = if (total > 0) "$CORNERSTONE_TITLE ${index + 1} of $total" else CORNERSTONE_TITLE
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
        CaseTable(
            viewableCase = case,
            modifier = Modifier.weight(1f, fill = false)
        )
        OutlinedCard(
            modifier = Modifier.padding(vertical = 10.dp),
            colors = androidx.compose.material3.CardDefaults.outlinedCardColors(
                containerColor = androidx.compose.ui.graphics.Color.White
            )
        ) {
            ReadonlyInterpretationView(
                case.viewableInterpretation,
                modifier = Modifier.fillMaxWidth(),
                handler = object : ReadonlyInterpretationViewHandler {}
            )
        }
    }
}
