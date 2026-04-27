package io.rippledown.caseview

class ColumnWidths(val numberOfDates: Int) {
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

    // Each per-episode value column gets the same weight regardless of how
    // many episodes the case has. The outer column layout
    // (attribute | scrollable area | range | units) always sums to 1.0 and
    // fills the panel exactly. Multiple episodes scroll horizontally inside
    // the scrollable area only.
    private val dataColumnWeight: Float =
        1.0F - attributeColumnWeight - valueRangeGapWeight - referenceRangeColumnWeight - unitsColumnWeight

    fun valueColumnWeight() = dataColumnWeight

    /**
     * Weight reserved for the horizontally scrollable area which holds the
     * date headers and per-episode values: one value cell ([valueColumnWeight])
     * plus the trailing value-to-range gap ([valueRangeGapWeight]). The
     * attribute column to the left and the reference-range / units columns
     * to the right stay fixed; only the contents of this slot scroll, with
     * the inner content N times wider for an N-episode case.
     */
    fun scrollableAreaWeight(): Float = dataColumnWeight + valueRangeGapWeight
}