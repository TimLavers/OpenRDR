package io.rippledown.casecontrol

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ListItem
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.rippledown.constants.caseview.CASELIST_ID
import io.rippledown.constants.caseview.CASE_NAME_PREFIX
import io.rippledown.main.Handler
import io.rippledown.model.CaseId

interface CaseSelectorHandler : Handler {
    var caseIds: List<CaseId>
    var selectCase: (id: Long) -> Unit
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
@Preview
fun CaseSelector(handler: CaseSelectorHandler) {
    val count = handler.caseIds.size
    val scrollState = rememberLazyListState()

    Row {
        LazyColumn(
            state = scrollState,
            modifier = Modifier
                .size(150.dp, 800.dp)
                .testTag(CASELIST_ID)
                .semantics {
                    contentDescription = CASELIST_ID
                }
        ) {
            items(count) { index ->
                val caseId = handler.caseIds[index]
                ListItem(
                    modifier = Modifier
                        .clickable {
                            handler.selectCase(caseId.id!!)
                        }
                        .semantics { contentDescription = "$CASE_NAME_PREFIX${caseId.name}" },
                    text = {
                        Text(
                            text = caseId.name
                        )
                    }
                )
            }
        }
        VerticalScrollbar(
            modifier = Modifier.requiredWidth(16.dp),
            adapter = rememberScrollbarAdapter(scrollState)
        )
    }
}

