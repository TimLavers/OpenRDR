package io.rippledown.casecontrol

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.rippledown.constants.caseview.CASELIST_ID
import io.rippledown.constants.caseview.CASE_NAME_PREFIX
import io.rippledown.model.CaseId

interface CaseSelectorHandler {
    var selectCase: (id: Long) -> Unit
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
@Preview
fun CaseSelector(caseIds: List<CaseId>, handler: CaseSelectorHandler) {
    val count = caseIds.size
    val scrollState = rememberLazyListState()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(800.dp)
            .background(Color.White)
    ) {
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
                val caseId = caseIds[index]
                Text(
                    text = caseId.name,
                    modifier = Modifier
                        .clickable {
                            handler.selectCase(caseId.id!!)
                        }
                        .semantics { contentDescription = "$CASE_NAME_PREFIX${caseId.name}" },
                )
            }
        }
        VerticalScrollbar(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .requiredWidth(10.dp)
                .padding(end = 5.dp),
            adapter = rememberScrollbarAdapter(scrollState)
        )
    }
}

