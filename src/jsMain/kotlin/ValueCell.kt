import io.rippledown.model.Attribute
import io.rippledown.model.ReferenceRange
import io.rippledown.model.TestResult
import react.FC
import react.Props
import react.css.css
import react.dom.html.ReactHTML

external interface ValueCellHandler: Props {
    var value: TestResult
    var attribute: Attribute
}
val ValueCell = FC<ValueCellHandler> {
    ReactHTML.td {
        +resultText(it.value)
        id = "attribute_value_cell_${it.attribute.name}"
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
fun rangeText(referenceRange: ReferenceRange?): String {
    if (referenceRange == null) {
        return ""
    }
    if (referenceRange.upperString == null) {
        return "(<${referenceRange.lowerString})"
    }
    if (referenceRange.lowerString == null) {
        return "(>${referenceRange.upperString})"
    }
    return "(${referenceRange.lowerString} - ${referenceRange.upperString})"
}