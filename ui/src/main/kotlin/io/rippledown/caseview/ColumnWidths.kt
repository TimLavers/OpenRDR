package io.rippledown.caseview

class ColumnWidths(numberOfDates: Int) {
    init {
        require(numberOfDates > 0) {
            "There must be a positive number of columns (i.e. dates)."
        }
    }

    val attributeColumnWeight = 0.28F

    // A blank gap rendered between the value column(s) and the reference
    // range column so right-aligned numeric values sit well clear of
    // their reference range.
    val valueRangeGapWeight = 0.24F
    val referenceRangeColumnWeight = 0.20F
    val unitsColumnWeight = 0.12F
    private val dataColumnWeight =
        ((1.0
                - attributeColumnWeight
                - valueRangeGapWeight
                - referenceRangeColumnWeight
                - unitsColumnWeight) / numberOfDates).toFloat()

    fun valueColumnWeight() = dataColumnWeight
}