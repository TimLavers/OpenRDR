package io.rippledown.caseview

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.rippledown.constants.caseview.CASEVIEW_CASE_NAME_ID
import io.rippledown.constants.caseview.CASE_HEADING
import io.rippledown.model.Attribute
import io.rippledown.model.caseview.ViewableCase

interface CaseViewHandler {
    var caseEdited: () -> Unit
    fun swapAttributes(moved: Attribute, target: Attribute)
}

/**
 * A view of a Case, including its name and a table of its attributes and results.
 *
 *  ORD2
 */
@Composable
fun CaseView(case: ViewableCase, handler: CaseViewHandler) {
    Column(
        modifier = Modifier
            .semantics {
                contentDescription = CASE_HEADING
            },
    ) {
        Text(
            text = case.name,
            style = MaterialTheme.typography.subtitle1,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            color = MaterialTheme.colors.primary,
            textAlign = TextAlign.Start,
            modifier = Modifier
                .testTag("$CASEVIEW_CASE_NAME_ID${case.name}")
                .semantics {
                    contentDescription = CASEVIEW_CASE_NAME_ID
                }
        )
        Spacer(modifier = Modifier.height(10.dp))
        CaseTable(case) { a: Attribute, b: Attribute ->
            handler.swapAttributes(a, b)
        }
    }
}