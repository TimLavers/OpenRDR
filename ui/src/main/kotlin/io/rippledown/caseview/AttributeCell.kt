package io.rippledown.caseview

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import io.rippledown.constants.caseview.ATTRIBUTE_CELL_DESCRIPTION_PREFIX
import io.rippledown.model.Attribute

@Composable
fun RowScope.AttributeCell(index: Int, caseName: String, attribute: Attribute, columnWidths: ColumnWidths) {
    Text(
        text = attribute.name,
        modifier = Modifier.weight(columnWidths.attributeColumnWeight)
            .semantics {
                contentDescription = attributeCellContentDescription(index, caseName)
            },
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.End
    )
}

fun attributeCellContentDescriptionPrefix(caseName: String) = "$ATTRIBUTE_CELL_DESCRIPTION_PREFIX $caseName"
fun attributeCellContentDescription(index: Int, caseName: String) =
    "${attributeCellContentDescriptionPrefix(caseName)} $index"