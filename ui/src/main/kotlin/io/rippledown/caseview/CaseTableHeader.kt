package io.rippledown.caseview

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ListItem
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.material.Text

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import io.rippledown.constants.caseview.CASEVIEW_CASE_NAME_ID
import io.rippledown.constants.caseview.CASE_NAME_PREFIX
import io.rippledown.model.caseview.ViewableCase

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun CaseHeader(viewableCase: ViewableCase) {
    Row {
        ListItem(
            text = {
                Text(
                    text = "Measurable"
                )
            }
        )
    }
}