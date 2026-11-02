package io.rippledown.kb.search

import io.kotest.matchers.shouldBe
import io.rippledown.kb.KB
import io.rippledown.kb.ai.AiEvaluationPolicy
import io.rippledown.model.AttributeKind
import io.rippledown.model.CaseListType
import io.rippledown.model.KBInfo
import io.rippledown.model.RDRCase
import io.rippledown.model.RDRCaseBuilder
import io.rippledown.model.condition.EpisodicCondition
import io.rippledown.model.condition.episodic.predicate.GreaterThan
import io.rippledown.model.condition.episodic.predicate.GreaterThanButWithDelay
import io.rippledown.model.condition.episodic.predicate.IsNotBlank
import io.rippledown.model.condition.episodic.signature.Current
import io.rippledown.model.rule.AiBoolean
import io.rippledown.model.rule.AssignValue
import io.rippledown.model.rule.ByDefinition
import io.rippledown.model.rule.CommentTemplate
import io.rippledown.persistence.inmemory.InMemoryKB
import io.rippledown.utils.defaultDate
import kotlin.test.Test

class CaseSearcherTest {
    private val kb = KB(InMemoryKB(KBInfo("searcher", "Searcher")))
    private val tsh = kb.attributeManager.getOrCreate("TSH")
    private val age = kb.attributeManager.getOrCreate("Age")
    private val searcher = CaseSearcher(kb)

    @Test
    fun `copies matching cases to the destination list, leaving the originals where they were`() {
        // Given processed cases, of which two match the condition
        val low = addCase("Case1", "2.1")
        val high1 = addCase("Case2", "6.7")
        val high2 = addCase("Case3", "8.9")

        // When a search for the matching cases is run
        val result = searcher.search(spec(listOf(tshGreaterThan(4.0))))

        // Then the matching cases are copied to the destination list, and the originals stay put
        result.foundCaseIds shouldBe listOf(high1.id, high2.id)
        result.terminated shouldBe false
        result.searchedListNames shouldBe listOf("Processed")
        kb.userDefinedCaseLists().single().let { list ->
            list.name shouldBe "found"
            list.caseIds.map { it.name }.toSet() shouldBe setOf("Case2", "Case3")
        }
        kb.processedCaseIds().map { it.name } shouldBe listOf("Case1", "Case2", "Case3")
        low.id shouldBe 1L
    }

    @Test
    fun `combines conditions with and`() {
        // Given cases of which only one satisfies both conditions
        addCase("Both", "8.9", "60")
        addCase("TSHOnly", "6.7", "30")
        addCase("AgeOnly", "2.1", "70")

        // When a search requiring both conditions is run
        val result = searcher.search(
            spec(listOf(tshGreaterThan(4.0), ageGreaterThan(50.0)))
        )

        // Then only the case satisfying both is found
        result.foundCaseIds.map { kb.getCase(it)?.name } shouldBe listOf("Both")
    }

    @Test
    fun `combines conditions with or`() {
        // Given cases of which three satisfy either condition and one satisfies neither
        addCase("Both", "8.9", "60")
        addCase("TSHOnly", "6.7", "30")
        addCase("AgeOnly", "2.1", "70")
        addCase("Neither", "2.1", "40")

        // When a search accepting either condition is run
        val result = searcher.search(
            spec(listOf(tshGreaterThan(4.0), ageGreaterThan(50.0)), connector = SearchConnector.ANY)
        )

        // Then the three satisfying cases are found
        result.foundCaseIds.map { kb.getCase(it)?.name }.toSet() shouldBe setOf("Both", "TSHOnly", "AgeOnly")
    }

    @Test
    fun `stops when the limit is reached`() {
        // Given three matching cases
        addCase("Case1", "6.7")
        addCase("Case2", "8.9")
        addCase("Case3", "9.9")

        // When a search for at most two of them is run
        val result = searcher.search(spec(listOf(tshGreaterThan(4.0)), limit = 2))

        // Then exactly two are found, and the search completes without being terminated
        result.foundCaseIds.size shouldBe 2
        result.terminated shouldBe false
        kb.userDefinedCaseLists().single().caseIds.size shouldBe 2
    }

    @Test
    fun `tries all cases when fewer match than the limit`() {
        // Given one matching case out of two
        addCase("Case1", "2.1")
        val matching = addCase("Case2", "6.7")

        // When a search for at most three cases is run
        val result = searcher.search(spec(listOf(tshGreaterThan(4.0)), limit = 3))

        // Then the lists are exhausted and the one matching case is found
        result.foundCaseIds shouldBe listOf(matching.id)
        result.terminated shouldBe false
    }

    @Test
    fun `searches only the lists given`() {
        // Given a matching processed case and a matching cornerstone case
        addCase("Processed1", "6.7")
        val cornerstone = kb.addCornerstoneCase(caseWithTsh("Cornerstone1", "8.9"))

        // When a search of the cornerstone list alone is run
        val result = searcher.search(spec(listOf(tshGreaterThan(4.0)), lists = listOf(CaseListType.Cornerstone)))

        // Then only the cornerstone case is found
        result.foundCaseIds shouldBe listOf(cornerstone.id)
        result.searchedListNames shouldBe listOf("Cornerstone")
    }

    @Test
    fun `interprets cases when a condition is on a comment`() {
        // Given a rule adding the comment Borderline to cases with raised TSH
        val commentAttribute = kb.attributeManager.createCommentAttribute()
        kb.derivedDefinitionManager.store(commentAttribute.id, CommentTemplate("Borderline"))
        val raisedTsh = kb.conditionManager.getOrCreate(EpisodicCondition(null, tsh, GreaterThan(4.0), Current))
        kb.ruleManager.createRuleAndAddToParent(
            kb.ruleTree.root,
            AssignValue(commentAttribute, ByDefinition),
            setOf(raisedTsh)
        )
        val borderline = addCase("Case1", "6.7")
        addCase("Case2", "2.1")

        // When a search for cases with that comment is run
        val result = searcher.search(
            spec(listOf(condition("Borderline is in case", commentAttribute, IsNotBlank)))
        )

        // Then the case whose interpretation carries the comment is found, and the other is not
        result.foundCaseIds shouldBe listOf(borderline.id)
    }

    @Test
    fun `a case whose interpretation needs an unprepared AI evaluation does not match`() {
        // Given an AI-evaluated attribute assigned by a rule, with no evaluation prepared
        val notes = kb.attributeManager.getOrCreate("clinical_notes")
        val hasDiabetes = kb.attributeManager.getOrCreate("hasDiabetes", AttributeKind.DERIVED)
        kb.derivedDefinitionManager.store(
            hasDiabetes.id,
            AiBoolean(notes, "Affirmative personal diabetes history", revision = "revision-1",
                evaluationProfile = AiEvaluationPolicy.defaultProfile("gemini-2.5-flash"))
        )
        kb.ruleManager.createRuleAndAddToParent(
            kb.ruleTree.root,
            AssignValue(hasDiabetes, ByDefinition),
            emptySet()
        )
        kb.addProcessedCase(withNotes("Case1", "T2 Diab"))

        // When a search for cases with that attribute in them is run
        val result = searcher.search(
            spec(listOf(condition("hasDiabetes is in case", hasDiabetes, IsNotBlank)))
        )

        // Then nothing is found - the search does not initiate classification - and no destination list is created
        result.foundCaseIds shouldBe emptyList()
        kb.userDefinedCaseLists() shouldBe emptyList()
    }

    @Test
    fun `retains the cases found before the search is cancelled`() {
        // Given three matching cases, each slow to evaluate
        addCase("Case1", "6.7")
        addCase("Case2", "8.9")
        addCase("Case3", "9.9")

        // When the search is cancelled after the first two cases have been tried
        var cancellationChecks = 0
        val result = searcher.search(
            spec(listOf(tshGreaterThan(4.0, delayMillis = 50))),
            isCancelled = { cancellationChecks++ >= 2 }
        )

        // Then the search is terminated, the two cases found first are retained, and the third is not tried
        result.terminated shouldBe true
        result.foundCaseIds.size shouldBe 2
        kb.userDefinedCaseLists().single().caseIds.size shouldBe 2
    }

    private fun spec(
        conditions: List<ParsedSearchCondition>,
        connector: SearchConnector = SearchConnector.ALL,
        limit: Int? = null,
        lists: List<CaseListType> = listOf(CaseListType.Processed),
        destination: String = "found"
    ) = CaseSearchSpec(conditions, connector, limit, lists, destination)

    private fun condition(text: String, attribute: io.rippledown.model.Attribute, predicate: io.rippledown.model.condition.episodic.predicate.TestResultPredicate) =
        ParsedSearchCondition(text, EpisodicCondition(null, attribute, predicate, Current))

    private fun tshGreaterThan(value: Double, delayMillis: Long? = null): ParsedSearchCondition {
        val predicate = if (delayMillis == null) GreaterThan(value) else GreaterThanButWithDelay(value, delayMillis)
        return condition("TSH is greater than $value", tsh, predicate)
    }

    private fun ageGreaterThan(value: Double) = condition("Age is greater than $value", age, GreaterThan(value))

    private fun addCase(name: String, tshValue: String, ageValue: String? = null): RDRCase {
        val builder = RDRCaseBuilder()
        builder.addValue(tsh, defaultDate, tshValue)
        ageValue?.let { builder.addValue(age, defaultDate, it) }
        return kb.addProcessedCase(builder.build(name))
    }

    private fun caseWithTsh(name: String, value: String): RDRCase {
        val builder = RDRCaseBuilder()
        builder.addValue(tsh, defaultDate, value)
        return builder.build(name)
    }

    private fun withNotes(name: String, text: String): RDRCase {
        val notes = kb.attributeManager.getOrCreate("clinical_notes")
        val builder = RDRCaseBuilder()
        builder.addValue(notes, defaultDate, text)
        return builder.build(name)
    }
}
