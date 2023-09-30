package io.rippledown.model.interpretationview

import io.kotest.assertions.withClue
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
    private lateinit var interp: Interpretation

    @BeforeTest
    fun init() {
        interp = Interpretation(caseId)
    }

    @Test
    fun constructorWithNoViewFields() {
        with(ViewableInterpretation(interp)) {
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
        interp.add(rule)
        ViewableInterpretation(interp).textGivenByRules() shouldBe conclusion.text
    }

    @Test
    fun textGivenByRulesWithDuplicateConclusion() {
        val conclusion = Conclusion(1, "First conclusion")
        val rule0 = Rule(0, null, conclusion, emptySet())
        val rule1 = Rule(1, null, conclusion, emptySet())
        interp.add(rule0)
        interp.add(rule1)
        ViewableInterpretation(interp).textGivenByRules() shouldBe conclusion.text
    }

    @Test
    fun textGivenByRulesWithNullRuleConclusion() {
        val conclusion = Conclusion(1, "First conclusion")
        val rule0 = Rule(0, null, conclusion, emptySet())
        val rule1 = Rule(1, null, null, emptySet())
        interp.add(rule0)
        interp.add(rule1)
        ViewableInterpretation(interp).textGivenByRules() shouldBe conclusion.text
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
        ViewableInterpretation(interpretation).textGivenByRules() shouldBe "A B C"
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
        ViewableInterpretation(interpretation).interpretation.idsOfRulesGivingConclusion(concA) shouldBe setOf(
            rule0.id,
            rule1.id
        )
        ViewableInterpretation(interpretation).interpretation.idsOfRulesGivingConclusion(concB) shouldBe setOf(
            rule2.id
        )
    }

    @Test
    fun serialisationWithVerifiedText() {
        val verified = "I can verify that is true."
        val view = ViewableInterpretation(interp).apply { verifiedText = verified }
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
        interp.apply { add(rule) }
        val view = ViewableInterpretation(interp)
        val restored = serializeDeserialize(view)
        restored.interpretation shouldBe interp
        restored.latestText() shouldBe conclusion.text
    }

    @Test
    fun serialisationWithRuleSummary() {
        val conclusion = Conclusion(1, "First conc")
        val conditions = setOf(
            Is(1, Attribute(1, "x"), "1"),
        )
        val rule = Rule(0, null, conclusion, conditions)
        val ruleSummary = rule.summary()
        interp.apply { add(ruleSummary) }
        val view = ViewableInterpretation(interp)
        withClue("sanity check") {
            view.textGivenByRules() shouldBe conclusion.text
        }
        val restored = serializeDeserialize(view)
        restored.interpretation shouldBe interp
        restored.textGivenByRules() shouldBe conclusion.text
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
        val view = ViewableInterpretation(interp)
        view.apply { this.diffList = diffList }
        val restored = serializeDeserialize(view)
        restored.diffList shouldBe diffList
    }

    @Test
    fun serialisationWithAllFields() {
        val verified = "I can verify that is true."
        val diffList = DiffList(
            listOf(
                Addition(verified),
                Removal("I can verify that is false."),
                Replacement("I can verify that is false.", verified),
                Unchanged("I can verify that is true or false.")
            )
        )
        val view = ViewableInterpretation(interp)
        view.apply {
            this.diffList = diffList
            this.verifiedText = verified
        }
        val restored = serializeDeserialize(view)
        restored.diffList shouldBe diffList
        restored.verifiedText shouldBe verified
        restored.latestText() shouldBe verified
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
        val view = ViewableInterpretation(interp)
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
        interp.add(rule0)
        interp.add(rule1)
        val view = ViewableInterpretation(interp)
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
        interp.add(rule0)
        val view = ViewableInterpretation(interp)
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
        interp.add(rule1)
        val view = ViewableInterpretation(interp)
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
        val view = ViewableInterpretation(interp)
        view.verifiedText = verifiedText
        interp.reset()
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
        val view = ViewableInterpretation(interp)
        view.diffList = diffList
        interp.reset()
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
        val interp = Interpretation(caseId)
        interp.add(rule0)
        val updatedView = ViewableInterpretation(interp, verifiedText = verifiedText)
        updatedView.latestText() shouldBe verifiedText
    }

    @Test
    fun latestTextShouldBeTheInterpretationIfNoVerifiedText() {
        val conclusion = Conclusion(1, "First conc")
        val conditions = setOf(
            Is(1, Attribute(1, "x"), "1"),
        )
        val rule0 = Rule(10, null, conclusion, conditions)
        val interp = Interpretation(caseId)
        interp.add(rule0)
        val updatedView = ViewableInterpretation(interp)
        updatedView.latestText() shouldBe conclusion.text
    }

    @Test
    fun serializedWithDiffList() {
        val diffList = DiffList(listOf(Addition("Coffee is very good")))
        val view = ViewableInterpretation(interp)
        view.diffList = diffList

        val sd = serializeDeserialize(view)
        sd shouldBe view
        sd.diffList shouldBe diffList
    }

    @Test
    fun serializedWithVerifiedText() {
        val text = "Coffee is very good"
        val view = ViewableInterpretation(interp)
        view.verifiedText = text
        val sd = serializeDeserialize(view)
        sd shouldBe view
        sd.verifiedText shouldBe text
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
