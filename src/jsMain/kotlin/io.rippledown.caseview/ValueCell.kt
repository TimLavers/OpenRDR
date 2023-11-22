package io.rippledown.caseview

import io.rippledown.main.px4
import io.rippledown.model.Attribute
import io.rippledown.model.ReferenceRange
import io.rippledown.model.TestResult
import mui.material.TableCell
import mui.system.sx
import react.FC
import react.Props

external interface ValueCellHandler : Props {
    var index: Int
    var value: TestResult
    var attribute: Attribute
}

val ValueCell = FC<ValueCellHandler> {
    TableCell {
        +resultText(it.value)
        id = "attribute_value_cell_${it.attribute.name}_${it.index}"
        sx {
            padding = px4
        }
    }
}

internal fun resultText(result: TestResult): String {
    val value = result.value.text
    return if (result.units == null) {
        value
    } else {
        "$value ${result.units}"
    }
}


val ReferenceRangeCell = FC<ValueCellHandler> {
    TableCell {
        +rangeText(it.value.referenceRange)
        id = "reference_range_cell_${it.attribute.name}"
        sx {
            padding = px4
        }
    }
}

fun rangeText(referenceRange: ReferenceRange?) =
    with(referenceRange) {
        when {
            this == null -> ""
            lowerString == null && upperString == null -> ""
            lowerString == null -> "< $upperString"
            upperString == null -> "> $lowerString"
            else -> "$lowerString - $upperString"
        }
    }