package io.rippledown.caseview

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import io.rippledown.constants.caseview.ATTRIBUTE_HEADER_CELL_DESCRIPTION
import io.rippledown.constants.caseview.ATTRIBUTE_HEADER_CELL_TEXT

@Composable
fun RowScope.AttributesHeaderCell(columnWidths: ColumnWidths) {
    Text(
        text = ATTRIBUTE_HEADER_CELL_TEXT,
        modifier = Modifier.weight(columnWidths.attributeColumnWeight)
            .semantics {
                contentDescription = ATTRIBUTE_HEADER_CELL_DESCRIPTION
            },
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.End
    )
}
