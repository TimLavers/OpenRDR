package io.rippledown.cornerstone

import InterpretationTabs
import InterpretationTabsHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.rippledown.caseview.CaseTable
import io.rippledown.constants.cornerstone.CORNERSTONE_CASE_NAME_ID
import io.rippledown.constants.cornerstone.CORNERSTONE_ID
import io.rippledown.constants.cornerstone.CORNERSTONE_TITLE
import io.rippledown.decoration.ItalicGrey
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.diff.Diff

@Composable
fun CornerstoneInspection(case: ViewableCase) {
    Column(
        verticalArrangement = Arrangement.Top,
        modifier = Modifier
            .fillMaxHeight()
            .padding(start = 5.dp)
            .width(500.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = CORNERSTONE_TITLE,
                style = ItalicGrey,
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
