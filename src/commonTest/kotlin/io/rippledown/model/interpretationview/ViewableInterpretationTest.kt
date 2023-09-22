package io.rippledown.model.interpretationview

import io.kotest.matchers.shouldBe
import io.rippledown.model.Attribute
import io.rippledown.model.CaseId
import io.rippledown.model.Conclusion
import io.rippledown.model.Interpretation
import io.rippledown.model.condition.ContainsText
import io.rippledown.model.condition.Is
import io.rippledown.model.diff.*
import io.rippledown.model.rule.Rule
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.BeforeTest
import kotlin.test.Test

class ViewableInterpretationTest {
    private val caseId = CaseId()
    private var attributeId = 0
    private var conditionId = 0
    private lateinit var interpretation: Interpretation
    private lateinit var view: ViewableInterpretation

    @BeforeTest
    fun init() {
        interpretation = Interpretation(caseId)
        view = ViewableInterpretation(interpretation)
    }

    @Test
    fun constructorWithNoViewFields() {
        with(view) {
            interpretation shouldBe interpretation
            verifiedText shouldBe null
            diffList shouldBe DiffList()
            latestText() shouldBe ""
            textGivenByRules() shouldBe ""
        }
    }

    @Test
    fun textGivenByRules() {
        val conclusion = Conclusion(1, "First conclusion")
        val rule = Rule(0, null, conclusion, emptySet())
        interpretation.add(rule)
        view.textGivenByRules() shouldBe conclusion.text
    }

    @Test
    fun textGivenByRulesWithDuplicateConclusion() {
        val conclusion = Conclusion(1, "First conclusion")
        val rule0 = Rule(0, null, conclusion, emptySet())
        val rule1 = Rule(1, null, conclusion, emptySet())
        interpretation.add(rule0)
        interpretation.add(rule1)
        view.textGivenByRules() shouldBe conclusion.text
    }

    @Test
    fun textGivenByRulesWithNullRuleConclusion() {
        val conclusion = Conclusion(1, "First conclusion")
        val rule0 = Rule(0, null, conclusion, emptySet())
        val rule1 = Rule(1, null, null, emptySet())
        interpretation.add(rule0)
        interpretation.add(rule1)
        view.textGivenByRules() shouldBe conclusion.text
    }

    @Test
    fun textGivenByRulesHasConclusionsInABOrder() {
        val interpretation = Interpretation(caseId)
        val rule0 = Rule(0, null, Conclusion(1, "C"), emptySet())
        val rule1 = Rule(1, null, Conclusion(2, "A"), emptySet())
        val rule2 = Rule(1, null, Conclusion(3, "B"), emptySet())
        interpretation.add(rule0)
        interpretation.add(rule1)
        interpretation.add(rule2)
        view.textGivenByRules() shouldBe "A B C"
    }

    @Test
    fun idsOfRulesGivingConclusion() {
        val interpretation = Interpretation(caseId)
        val concA = Conclusion(1, "A")
        val concB = Conclusion(2, "B")
        val rule0 = Rule(0, null, concA, emptySet())
        val rule1 = Rule(1, null, concA, emptySet())
        val rule2 = Rule(2, null, concB, emptySet())
        interpretation.idsOfRulesGivingConclusion(concA) shouldBe setOf()

        interpretation.add(rule0)
        interpretation.add(rule1)
        interpretation.add(rule2)
        interpretation.idsOfRulesGivingConclusion(concA) shouldBe setOf(rule0.id, rule1.id)
        interpretation.idsOfRulesGivingConclusion(concB) shouldBe setOf(rule2.id)
    }

    @Test
    fun serialisationWithVerifiedText() {
        val verified = "I can verify that is true."
        view.apply { verifiedText = verified }
        val restored = serializeDeserialize(view)
        restored.verifiedText shouldBe verified
    }

    @Test
    fun serialisationWithInterpretation() {
        val conclusion = Conclusion(1, "First conc")
        val conditions = setOf(
            Is(1, Attribute(1, "x"), "1"),
        )
        val rule = Rule(0, null, conclusion, conditions)
        interpretation.apply { add(rule) }
        val restored = serializeDeserialize(view)
        restored.interpretation shouldBe interpretation
    }

    @Test
    fun serialisationWithRuleSummary() {
        val conclusion = Conclusion(1, "First conc")
        val conditions = setOf(
            Is(1, Attribute(1, "x"), "1"),
        )
        val rule = Rule(0, null, conclusion, conditions)
        val ruleSummary = rule.summary()
        interpretation.apply { add(ruleSummary) }
        val restored = serializeDeserialize(view)
        restored.interpretation shouldBe interpretation
    }

    @Test
    fun serialisationWithDiffList() {
        val diffList = DiffList(
            listOf(
                Addition("I can verify that is true."),
                Removal("I can verify that is false."),
                Replacement("I can verify that is false.", "I can verify that is true."),
                Unchanged("I can verify that is true or false.")
            )
        )
        view.apply { this.diffList = diffList }
        val restored = serializeDeserialize(view)
        restored.diffList shouldBe diffList
    }

    @Test
    fun serialisationWithDiffListModifiedAfterInterpretationIsConstructed() {
        val diffList = DiffList(
            listOf(
                Addition("I can verify that is true."),
                Removal("I can verify that is false."),
                Replacement("I can verify that is false.", "I can verify that is true."),
                Unchanged("I can verify that is true or false.")
            )
        )
        view.apply { this.diffList = diffList }
        val restored = serializeDeserialize(view)
        restored.diffList shouldBe diffList
    }

    @Test
    fun shouldReturnConditionsForConclusion() {
        val c0 = Conclusion(1, "First conc")
        val conditions0 = setOf(
            containsText(Attribute(attributeId++, "A"), "text A"), containsText(
                Attribute(
                    attributeId++,
                    "B"
                ), "text B"
            )
        )
        val rule0 = Rule(0, null, c0, conditions0)
        val c1 = Conclusion(2, "Second conc")
        val conditions1 = setOf(
            containsText(Attribute(attributeId++, "C"), "text C"), containsText(
                Attribute(
                    attributeId++,
                    "D"
                ), "text D"
            )
        )
        val rule1 = Rule(1, null, c1, conditions1)
        interpretation.add(rule0)
        interpretation.add(rule1)
        view.conditionsForConclusion(c0) shouldBe listOf("A contains \"text A\"", "B contains \"text B\"")
        view.conditionsForConclusion(c1) shouldBe listOf("C contains \"text C\"", "D contains \"text D\"")
    }

    @Test
    fun conditionsForConclusionShouldBeInAlphaOrderForTheLeafRule() {
        val conclusion = Conclusion(1, "First conc")
        val conditions = setOf(
            containsText(Attribute(attributeId++, "z"), "text z"),
            containsText(Attribute(attributeId++, "A"), "text A"),
            containsText(Attribute(attributeId++, "Y"), "text Y"),
            containsText(Attribute(attributeId++, "b"), "text b"),
        )
        val rule0 = Rule(0, null, conclusion, conditions)
        interpretation.add(rule0)
        view.conditionsForConclusion(conclusion) shouldBe listOf(
            "A contains \"text A\"",
            "b contains \"text b\"",
            "Y contains \"text Y\"",
            "z contains \"text z\""
        )
    }

    @Test
    fun conditionsForConclusionShouldListConditionsOfParentRulesFirst() {
        val conclusion0 = Conclusion(1, "First conc")
        val conclusion1 = Conclusion(2, "Second conc")
        val conditions0 = setOf(
            ContainsText(1, Attribute(26, "z"), "text z"),
            ContainsText(2, Attribute(1, "A"), "text A"),
            ContainsText(3, Attribute(25, "Y"), "text Y"),
            ContainsText(4, Attribute(2, "b"), "text b"),
        )
        val conditions1 = setOf(
            ContainsText(5, Attribute(18, "r"), "text r"),
            ContainsText(6, Attribute(19, "s"), "text s"),
            ContainsText(7, Attribute(16, "p"), "text p"),
            ContainsText(8, Attribute(17, "q"), "text q"),
        )
        val rule0 = Rule(0, null, conclusion0, conditions0)
        val rule1 = Rule(1, rule0, conclusion1, conditions1)
        interpretation.add(rule1)
        view.conditionsForConclusion(conclusion1) shouldBe listOf(
            "A contains \"text A\"",
            "b contains \"text b\"",
            "Y contains \"text Y\"",
            "z contains \"text z\"",
            "p contains \"text p\"",
            "q contains \"text q\"",
            "r contains \"text r\"",
            "s contains \"text s\""
        )
    }

    @Test
    fun resettingTheInterpretationShouldNotChangeTheVerifiedText() {
        val verifiedText = "I can verify that is true."
        view.verifiedText = verifiedText
        interpretation.reset()
        view.verifiedText shouldBe verifiedText
    }

    @Test
    fun resettingTheInterpretationShouldNotChangeTheDiffList() {
        val diffList = DiffList(
            listOf(
                Addition("I can verify that is true."),
                Removal("I can verify that is false."),
                Replacement("I can verify that is false.", "I can verify that is true."),
                Unchanged("I can verify that is true or false.")
            )
        )
        view.diffList = diffList
        interpretation.reset()
        view.diffList shouldBe diffList
    }

    @Test
    fun latestTextShouldBeTheVerifiedTextIfNotNull() {
        val verifiedText = "I can verify that is true."
        val conclusion = Conclusion(1, "First conc")
        val conditions = setOf(
            Is(1, Attribute(1, "x"), "1"),
        )
        val rule0 = Rule(10, null, conclusion, conditions)
        with(Interpretation(caseId)) {
            add(rule0)
            latestText() shouldBe verifiedText
        }
    }

    @Test
    fun latestTextShouldBeTheInterpretationIfNoVerifiedText() {
        val conclusion = Conclusion(1, "First conc")
        val conditions = setOf(
            Is(1, Attribute(1, "x"), "1"),
        )
        val rule0 = Rule(10, null, conclusion, conditions)
        with(Interpretation(caseId)) {
            add(rule0)
            latestText() shouldBe conclusion.text
        }
    }

    private fun containsText(attribute: Attribute, match: String): ContainsText {
        return ContainsText(conditionId++, attribute, match)
    }

    private fun serializeDeserialize(viewableInterpretation: ViewableInterpretation): ViewableInterpretation {
        val format = Json {
            allowStructuredMapKeys = true
            prettyPrint = true
        }
        val serialized = format.encodeToString(viewableInterpretation)
        return format.decodeFromString(serialized)
    }

}