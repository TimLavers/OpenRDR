package io.rippledown.caseview

import io.rippledown.model.Attribute
import io.rippledown.model.caseview.ViewableCase

/**
 * True when [attribute] should be displayed under the case-view filter [query].
 *
 * - A blank query matches every attribute (filter is inactive).
 * - Otherwise, matching is case-insensitive substring search against anything
 *   visible on the attribute's row: the attribute's own name, and for each
 *   episode the value text, the reference range bounds, and the units.
 *
 * The filter is owned at the case-control level and applied uniformly to the
 * current case and any cornerstone case displayed alongside it, so this helper
 * is intentionally case-agnostic in everything except the per-attribute lookup.
 */
fun matchesFilter(case: ViewableCase, attribute: Attribute, query: String): Boolean {
    if (query.isBlank()) return true
    val needle = query.trim().lowercase()
    if (attribute.name.lowercase().contains(needle)) return true
    val results = case.case.resultsFor(attribute) ?: return false
    return results.any { result ->
        if (result.value.text.lowercase().contains(needle)) return@any true
        val range = result.referenceRange
        if (range != null) {
            if (range.lowerString?.lowercase()?.contains(needle) == true) return@any true
            if (range.upperString?.lowercase()?.contains(needle) == true) return@any true
        }
        if (result.units?.lowercase()?.contains(needle) == true) return@any true
        false
    }
}
