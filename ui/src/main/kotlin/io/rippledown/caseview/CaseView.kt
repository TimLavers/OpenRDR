 package io.rippledown.caseview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.rippledown.constants.caseview.CASEVIEW_CASE_NAME_ID
import io.rippledown.constants.caseview.CASE_HEADING
import io.rippledown.main.Handler
import io.rippledown.model.caseview.ViewableCase

interface CaseViewHandler : Handler {
    var case: ViewableCase
    fun caseEdited()
}

/**
 * A view of a Case, including its name and a table of its attributes and results.
 *
 *  ORD2
 */
@Composable
fun CaseView(handler: CaseViewHandler) {

    Column(
        modifier = Modifier
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        println("drawing case: ${handler.case.name}")
        Text(
            text = handler.case.name,
            style = MaterialTheme.typography.subtitle1,
            textAlign = TextAlign.Start,
            modifier = Modifier
                .testTag("$CASEVIEW_CASE_NAME_ID${handler.case.name}")
                .semantics {
                    contentDescription = CASEVIEW_CASE_NAME_ID
                }
        )
        CaseTable(object : CaseTableHandler {
            override val viewableCase = handler.case
        })
    }
}