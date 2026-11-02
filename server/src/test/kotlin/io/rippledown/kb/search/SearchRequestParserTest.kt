package io.rippledown.kb.search

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.rippledown.kb.ConditionParser
import io.rippledown.kb.KB
import io.rippledown.model.Attribute
import io.rippledown.model.CaseListType
import io.rippledown.model.KBInfo
import io.rippledown.model.RDRCaseBuilder
import io.rippledown.model.condition.EpisodicCondition
import io.rippledown.model.condition.episodic.predicate.GreaterThan
import io.rippledown.model.condition.episodic.predicate.GreaterThanButWithDelay
import io.rippledown.model.condition.episodic.signature.Current
import io.rippledown.persistence.inmemory.InMemoryKB
import io.rippledown.utils.defaultDate
import kotlin.test.Test

class SearchRequestParserTest {
    private val kb = KB(InMemoryKB(KBInfo("search_parser", "Search parser")))
    private val tsh = kb.attributeManager.getOrCreate("TSH")
    private val parser = SearchRequestParser(kb, fakeConditionParser())

    @Test
    fun `parses the search parameters`() {
        // Given a search request in the form the chat action receives
        // When it is parsed
        val spec = parse()

        // Then every parameter is carried by the spec
        spec.connector shouldBe SearchConnector.ALL
        spec.limit shouldBe null
        spec.sourceLists shouldBe listOf(CaseListType.Processed)
        spec.destination shouldBe "highTSH"
    }

    @Test
    fun `keeps the user's wording of each condition`() {
        // Given a search with one condition
        // When it is parsed
        val spec = parse()

        // Then the condition's user text is kept alongside the condition itself
        val parsed = spec.conditions.single()
        parsed.userText shouldBe "TSH is greater than 4"
        parsed.condition shouldBe EpisodicCondition(null, tsh, GreaterThan(4.0), Current)
    }

    @Test
    fun `parses the delayed comparison phrase deterministically`() {
        // Given a search using the test-only delayed comparison
        // When it is parsed
        val spec = parse(conditionTexts = listOf("TSH is greater than but with delay 4"))

        // Then the condition is the delayed predicate, not whatever the condition parser would make of the phrase
        val condition = spec.conditions.single().condition as EpisodicCondition
        condition.predicate shouldBe GreaterThanButWithDelay(4.0)
        condition.attribute shouldBe tsh
    }

    @Test
    fun `refuses a condition naming an unknown attribute, echoing it`() {
        // Given a search condition that names an attribute the KB does not have
        // When the request is parsed
        // Then it is refused, echoing the condition
        val error = shouldThrow<IllegalArgumentException> {
            parse(conditionTexts = listOf("Myrtle is greater than 5"))
        }
        error.message shouldContain "Cannot search"
        error.message shouldContain "Myrtle"
    }

    @Test
    fun `refuses a condition that cannot be parsed, echoing it`() {
        // Given a search condition that corresponds to no condition
        // When the request is parsed
        // Then it is refused, echoing the condition
        val error = shouldThrow<IllegalArgumentException> {
            parse(conditionTexts = listOf("whatever"))
        }
        error.message shouldContain "Cannot search"
        error.message shouldContain "whatever"
    }

    @Test
    fun `refuses a search with no conditions`() {
        // Given a search with no conditions
        // When the request is parsed
        // Then it is refused
        shouldThrow<IllegalArgumentException> {
            parse(conditionTexts = emptyList())
        }.message shouldContain "at least one condition"
    }

    @Test
    fun `accepts and and or in any case, and refuses any other connector`() {
        // Given connectors spelled in various ways
        // When requests using them are parsed
        parse(connectorText = "and").connector shouldBe SearchConnector.ALL
        parse(connectorText = "AND").connector shouldBe SearchConnector.ALL
        parse(connectorText = "&").connector shouldBe SearchConnector.ALL
        parse(connectorText = "^").connector shouldBe SearchConnector.ALL
        parse(connectorText = "or").connector shouldBe SearchConnector.ANY
        parse(connectorText = "Or").connector shouldBe SearchConnector.ANY
        parse(connectorText = "|").connector shouldBe SearchConnector.ANY
        parse(connectorText = "||").connector shouldBe SearchConnector.ANY

        // Then any other connector is refused
        shouldThrow<IllegalArgumentException> {
            parse(connectorText = "xor")
        }.message shouldContain "and"
    }

    @Test
    fun `refuses a limit that is not at least one`() {
        // Given limits of zero and below
        // When requests using them are parsed
        // Then they are refused, while a positive limit and no limit at all are accepted
        shouldThrow<IllegalArgumentException> { parse(limit = 0) }
        shouldThrow<IllegalArgumentException> { parse(limit = -1) }
        parse(limit = 3).limit shouldBe 3
        parse(limit = null).limit shouldBe null
    }

    @Test
    fun `resolves the built-in lists without regard to case`() {
        // Given list names for the built-in lists in various spellings
        // When requests using them are parsed
        // Then they resolve to the built-in lists, once each
        parse(listNames = listOf("processed")).sourceLists shouldBe listOf(CaseListType.Processed)
        parse(listNames = listOf("Processed")).sourceLists shouldBe listOf(CaseListType.Processed)
        parse(listNames = listOf("cornerstones")).sourceLists shouldBe listOf(CaseListType.Cornerstone)
        parse(listNames = listOf("Cornerstone Cases")).sourceLists shouldBe listOf(CaseListType.Cornerstone)
        parse(listNames = listOf("processed", "processed")).sourceLists shouldBe listOf(CaseListType.Processed)
    }

    @Test
    fun `resolves an existing user-defined list without regard to case`() {
        // Given a user-defined list named Good
        val case = kb.addProcessedCase(RDRCaseBuilder().build("Case1"))
        kb.copyCaseToList(case.id!!, "Good")

        // When a search names it in a different case
        val spec = parse(listNames = listOf("good"))

        // Then it resolves to the existing list's spelling
        spec.sourceLists shouldBe listOf(CaseListType("Good"))
    }

    @Test
    fun `refuses an unknown list, echoing it`() {
        // Given a list name that is no list of this KB
        // When the request is parsed
        // Then it is refused, echoing the name
        val error = shouldThrow<IllegalArgumentException> {
            parse(listNames = listOf("nonexistent"))
        }
        error.message shouldContain "Cannot search"
        error.message shouldContain "nonexistent"
    }

    @Test
    fun `refuses a search with no lists, or a blank list name`() {
        // Given a search with no lists, or a blank name
        // When the request is parsed
        // Then it is refused
        shouldThrow<IllegalArgumentException> {
            parse(listNames = emptyList())
        }.message shouldContain "without any case lists"
        shouldThrow<IllegalArgumentException> {
            parse(listNames = listOf("  "))
        }
    }

    @Test
    fun `refuses a blank destination`() {
        // Given a blank destination list name
        // When the request is parsed
        // Then it is refused
        shouldThrow<IllegalArgumentException> {
            parse(destination = "   ")
        }.message shouldContain "blank"
    }

    @Test
    fun `refuses a destination naming a built-in list`() {
        // Given destination names that denote the built-in lists
        // When the request is parsed
        // Then it is refused, echoing the name
        listOf("Processed", "processed", "Cornerstone Cases").forEach { destination ->
            val error = shouldThrow<IllegalArgumentException> {
                parse(destination = destination)
            }
            error.message shouldContain "Cannot"
            error.message shouldContain destination.trim()
        }
    }

    private fun parse(
        conditionTexts: List<String> = listOf("TSH is greater than 4"),
        connectorText: String = "and",
        limit: Int? = null,
        listNames: List<String> = listOf("processed"),
        destination: String = "highTSH"
    ) = parser.parse(conditionTexts, connectorText, limit, listNames, destination)

    // A stand-in for the ConditionGenerator-backed parser, deterministic so
    // that no LLM is needed. Unknown attributes make the attribute resolver
    // throw, which the real generator swallows into a null result, as does
    // this one.
    private fun fakeConditionParser(): ConditionParser = object : ConditionParser {
        override fun parse(expression: String, attributeFor: (String) -> Attribute): EpisodicCondition? {
            return try {
                val match = Regex("^(.+) is greater than ([0-9]+(?:\\.[0-9]+)?)$").find(expression.trim())
                    ?: return null
                val (attributeName, value) = match.destructured
                EpisodicCondition(null, attributeFor(attributeName), GreaterThan(value.toDouble()), Current)
            } catch (_: IllegalArgumentException) {
                null
            }
        }
    }
}
