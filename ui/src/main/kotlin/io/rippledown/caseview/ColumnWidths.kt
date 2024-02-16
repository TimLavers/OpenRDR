package io.rippledown.caseview

class ColumnWidths(numberOfDates: Int) {
    init {
        require(numberOfDates > 0) {
            "There must be a positive number of columns."
        }
    }
    val attributeColumnWeight = 0.2F
    val referenceRangeColumnWeight = 0.2F
    private val dataColumnWeight = ((1.0 - attributeColumnWeight - referenceRangeColumnWeight) / numberOfDates).toFloat()

    fun valueColumnWeight() = dataColumnWeight
}