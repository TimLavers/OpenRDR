package io.rippledown.model

/**
 * A list of TestResults for an Attribute in a case,
 * and having the same (or no) units.
 */
data class ResultsList(val results: List<TestResult>): List<TestResult> by results {
    val units: String?
    init {
        // Check that there is at least one entry.
        require(results.isNotEmpty())
        // Check that all entries have the same (or no) units.
        // (Maybe units should be part of the attribute.)
        var unitsFound: String? = null
        results.map {
            if (it.units != null) {
                if (unitsFound != null) {
                    require(unitsFound == it.units){
                        "Only one value for units is allowed in a ResultsList."
                    }
                } else {
                    unitsFound = it.units
                }
            }
        }
        units = unitsFound
    }
}