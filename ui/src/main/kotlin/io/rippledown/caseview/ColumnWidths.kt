package io.rippledown.caseview

class ColumnWidths(numberOfDates: Int) {
    init {
        require(numberOfDates > 0) {
            "There must be a positive number of columns (i.e. dates)."
        }
    }

    val attributeColumnWeight = 0.3F
    val referenceRangeColumnWeight = 0.2F
    val unitsColumnWeight = 0.1F
    private val dataColumnWeight =
        ((1.0 - attributeColumnWeight - referenceRangeColumnWeight - unitsColumnWeight) / numberOfDates).toFloat()

    fun valueColumnWeight() = dataColumnWeight
}