package io.rippledown.kb.search

import io.rippledown.model.CaseListType
import io.rippledown.model.condition.Condition

/**
 * How multiple search conditions are combined: all of them must hold, or any
 * of them. The requirements allow only a single connector across all the
 * conditions of one search.
 */
enum class SearchConnector { ALL, ANY }

/**
 * A search condition with the wording the user gave for it, so that the
 * search report can show both the user's form and the formal form
 * ([Condition.asText]).
 */
data class ParsedSearchCondition(val userText: String, val condition: Condition)

/**
 * The fully validated parameters of one case search: the parsed conditions,
 * how they are combined, the maximum number of cases to find (null for no
 * limit), the lists to search, and the name of the list the cases found are
 * copied into.
 */
data class CaseSearchSpec(
    val conditions: List<ParsedSearchCondition>,
    val connector: SearchConnector,
    val limit: Int?,
    val sourceLists: List<CaseListType>,
    val destination: String
)
