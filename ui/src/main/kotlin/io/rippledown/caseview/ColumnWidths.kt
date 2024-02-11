package io.rippledown.caseview

class ColumnWidths(val numberOfDates: Int) {
    val attributeColumnWeight = 0.2F
    val dataColumnWeight = ((1.0 - attributeColumnWeight) / numberOfDates).toFloat()

    fun columnWeight(index: Int): Float = when (index) {
        0 -> attributeColumnWeight
        else -> dataColumnWeight
    }


}