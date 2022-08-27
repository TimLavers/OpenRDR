import io.rippledown.model.Attribute
import io.rippledown.model.ReferenceRange
import io.rippledown.model.TestResult
import react.FC
import react.Props
import react.css.css
import react.dom.html.ReactHTML

external interface ValueCellHandler: Props {
    var index: Int
    var value: TestResult
    var attribute: Attribute
}
val ValueCell = FC<ValueCellHandler> {
    ReactHTML.td {
        +resultText(it.value)
        id = "attribute_value_cell_${it.attribute.name}_${it.index}"
        css {
            padding = px8
        }
    }
}
fun resultText(result: TestResult): String {
    val unit = result.units ?: ""
    return "${result.value.text} $unit"
}

val ReferenceRangeCell = FC<ValueCellHandler> {
    ReactHTML.td {
        +rangeText(it.value.referenceRange)
        id = "reference_range_cell_${it.attribute.name}"
        css {
            padding = px8
        }
    }
}
fun rangeText(referenceRange: ReferenceRange?) =
    with(referenceRange) {
        when {
            this == null -> ""
            lowerString == null && upperString == null -> ""
            lowerString == null -> "(> $upperString)"
            upperString == null -> "(< $lowerString)"
            else -> "($lowerString - $upperString)"
        }
    }