package io.rippledown.caseview

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
import io.rippledown.constants.caseview.CASEVIEW_CASE_NAME_ID
import io.rippledown.constants.caseview.CASE_HEADING
import io.rippledown.model.Attribute
import io.rippledown.model.caseview.ViewableCase

interface CaseViewHandler {
    fun swapAttributes(moved: Attribute, target: Attribute)
}

/**
 * A view of a Case, including its name and a table of its attributes and results.
 *
 *  ORD2
 */
@Composable
fun CaseView(case: ViewableCase, handler: CaseViewHandler, modifier: Modifier = Modifier) {
    val columnWidths = ColumnWidths(case.numberOfColumns)
    Column(
        modifier = modifier
            .semantics {
                contentDescription = CASE_HEADING
            },
    ) {
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
        CaseTable(
            viewableCase = case
        ) { a: Attribute, b: Attribute ->
            handler.swapAttributes(a, b)
        }
    }
}