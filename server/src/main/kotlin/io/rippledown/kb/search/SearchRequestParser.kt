package io.rippledown.kb.search

import io.rippledown.hints.ConditionChatService
import io.rippledown.hints.ConditionGenerator
import io.rippledown.kb.ConditionParser
import io.rippledown.kb.KB
import io.rippledown.model.Attribute
import io.rippledown.model.CaseListType
import io.rippledown.model.condition.EpisodicCondition
import io.rippledown.model.condition.episodic.predicate.GreaterThanButWithDelay
import io.rippledown.model.condition.episodic.signature.Current

/**
 * Parses the parameters of a search request, as the chat action receives
 * them, into a validated [CaseSearchSpec]. Anything invalid is refused with
 * an [IllegalArgumentException] whose message is shown to the user, echoing
 * the offending condition, list or destination.
 *
 * Condition texts are parsed by the same generator-backed parser that rule
 * building uses, but the attribute resolver accepts only attributes that
 * exist: a search condition may name any attribute of the KB, need not hold
 * for any particular case, and no rule session is involved. The test-only
 * delayed comparison is recognised here deterministically, before the
 * condition parser is consulted.
 *
 * The KB reads inside parsing are made under [lock], so that they do not race
 * other operations on the KB; the LLM translation inside the condition parser
 * runs without it.
 */
class SearchRequestParser(
    private val kb: KB,
    conditionParser: ConditionParser? = null,
    private val lock: SearchLock = SearchLock.NONE
) {
    private val parser: ConditionParser = conditionParser ?: generatorBackedConditionParser(kb, lock)

    fun parse(
        conditionTexts: List<String>,
        connectorText: String,
        limit: Int?,
        listNames: List<String>,
        destination: String
    ): CaseSearchSpec {
        require(conditionTexts.isNotEmpty()) { "Cannot search without at least one condition." }
        val conditions = conditionTexts.map { parseCondition(it) }
        val connector = parseConnector(connectorText)
        require(limit == null || limit > 0) { "Cannot search for a number of cases that is not at least 1." }
        val sourceLists = resolveLists(listNames)
        requireDestination(destination)
        return CaseSearchSpec(conditions, connector, limit, sourceLists, destination.trim())
    }

    private fun parseCondition(text: String): ParsedSearchCondition {
        val trimmed = text.trim()
        delayedComparisonPattern.find(trimmed)?.let { match ->
            val (attributeName, value) = match.destructured
            val attribute = existingAttribute(attributeName)
            val predicate = GreaterThanButWithDelay(value.toDouble())
            return ParsedSearchCondition(trimmed, EpisodicCondition(null, attribute, predicate, Current, trimmed))
        }
        val condition = try {
            parser.parse(trimmed, ::existingAttribute)
        } catch (_: Exception) {
            null
        }
        return ParsedSearchCondition(
            trimmed,
            requireNotNull(condition) {
                "Cannot search: the condition \"$trimmed\" does not correspond to a condition in this knowledge base."
            }
        )
    }

    /**
     * Resolves the attribute the condition names, refusing a name that is no
     * attribute of this KB. The generator-backed parser swallows the throw,
     * so the refusal is reported by [parseCondition] with the whole condition.
     */
    private fun existingAttribute(name: String): Attribute =
        lock.withLock { kb.attributeManager.byName(name) }
            ?: throw IllegalArgumentException("There is no attribute named \"$name\".")

    private fun parseConnector(text: String): SearchConnector = when (text.trim().lowercase()) {
        "and", "&", "^" -> SearchConnector.ALL
        "or", "|", "||" -> SearchConnector.ANY
        else -> throw IllegalArgumentException("Cannot search: the conditions must be joined by \"and\" or \"or\".")
    }

    private fun resolveLists(names: List<String>): List<CaseListType> {
        require(names.isNotEmpty()) { "Cannot search without any case lists to search." }
        val resolved = mutableListOf<CaseListType>()
        names.forEach { name ->
            val trimmed = name.trim()
            require(trimmed.isNotEmpty()) { "Cannot search a case list with a blank name." }
            val listType = when {
                CaseListType.isReservedListName(trimmed) ->
                    if (trimmed.lowercase().contains("cornerstone")) CaseListType.Cornerstone else CaseListType.Processed

                else -> lock.withLock { kb.userDefinedCaseLists() }
                    .firstOrNull { it.name.equals(trimmed, ignoreCase = true) }
                    ?.let { CaseListType(it.name) }
                    ?: throw IllegalArgumentException("Cannot search: there is no case list named \"$trimmed\".")
            }
            if (resolved.none { it == listType }) resolved.add(listType)
        }
        return resolved
    }

    private fun requireDestination(destination: String) {
        val trimmed = destination.trim()
        require(trimmed.isNotEmpty()) { "Cannot search for cases to go into a list with a blank name." }
        require(!CaseListType.isReservedListName(trimmed)) {
            "Cannot put the search results in \"$trimmed\" as that is the name of a built-in list."
        }
    }

    companion object {
        // The test-only delayed comparison, e.g. "TSH is greater than but with delay 4".
        private val delayedComparisonPattern =
            Regex("^(.+) is greater than but with delay ([0-9]+(?:\\.[0-9]+)?)$")
    }
}

/**
 * The parser rule building uses for a user's own words, with the same chat
 * service and the KB's attribute names.
 */
private fun generatorBackedConditionParser(kb: KB, lock: SearchLock): ConditionParser {
    val conditionChatService = ConditionChatService()
    return object : ConditionParser {
        override fun parse(expression: String, attributeFor: (String) -> Attribute): io.rippledown.model.condition.Condition? {
            val attributeNames = lock.withLock { kb.attributeNames() }
            return ConditionGenerator(attributeFor, conditionChatService, attributeNames).conditionFor(expression)
        }
    }
}
