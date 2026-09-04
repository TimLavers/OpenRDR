package io.rippledown.kb.chat

import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.rippledown.model.*
import io.rippledown.model.interpretationview.ViewableInterpretation
import io.rippledown.model.rule.AssignValue
import io.rippledown.model.rule.ByDefinition
import io.rippledown.model.rule.CommentTemplate
import io.rippledown.model.rule.RuleSummary
import io.rippledown.utils.AttributeWithValue
import io.rippledown.utils.createCaseWithInterpretation
import io.rippledown.utils.createViewableCase
import kotlin.test.Test

class KBChatServiceTest {
    @Test
    fun `system instruction should not contain placeholders`() {
        // Given
        val case = createCaseWithInterpretation("Test Case")

        // When
        val systemPrompt = KBChatService.systemPrompt(case)

        // Then
        systemPrompt shouldNotContain "{{"
        systemPrompt shouldNotContain "}}"
    }

    @Test
    fun `system instruction should contain the comments in the interpretation`() {
        // Given
        val comments = listOf("Go to Bondi", "Go to Malabar")
        val case = createCaseWithInterpretation("Test Case", commentTexts = comments)

        // When
        val systemPrompt = KBChatService.systemPrompt(case)

        // Then
        comments.forEach { systemPrompt shouldContain it }
    }

    @Test
    fun `system instruction should contain comments given as comment-attribute assignments`() {
        // Given a case whose raw interpretation holds an unresolved ByDefinition
        // comment assignment, and whose viewable interpretation holds the
        // resolved copy, as produced by KB.viewableCase
        val c1 = Attribute(10, "C1", AttributeKind.COMMENT)
        val case = createCaseWithInterpretation("Test Case")
        case.case.interpretation.add(RuleSummary(id = 1, assignment = AssignValue(c1, ByDefinition)))
        val resolved = Interpretation(case.case.caseId).apply {
            add(RuleSummary(id = 1, assignment = AssignValue(c1, CommentTemplate("Surf's up."))))
        }
        case.viewableInterpretation = ViewableInterpretation(resolved)

        // When
        val systemPrompt = KBChatService.systemPrompt(case)

        // Then
        systemPrompt shouldContain "Surf's up."
    }

    @Test
    fun `system instruction should contains the case attributes in view order`() {
        val glucose = Attribute(1, "glucose")
        val glucoseValue = AttributeWithValue(glucose, Result("5.1"))
        val lipids = Attribute(2, "lipids")
        val lipidValue = AttributeWithValue(lipids, Result("5.2"))
        val age = Attribute(3, "age")
        val ageValue = AttributeWithValue(age, Result("53"))
        val case = createViewableCase(CaseId(99, "Case1"), listOf(glucoseValue, lipidValue, ageValue))

        val systemPrompt = KBChatService.systemPrompt(case)

        systemPrompt shouldContain glucose.name + "\n" + lipids.name + "\n" + age.name
    }

    @Test
    fun `system instruction should contain all KB attributes including those not on the current case`() {
        // Given a case with glucose and lipids, and a KB that also has haemoglobin and sodium
        val glucose = Attribute(1, "glucose")
        val glucoseValue = AttributeWithValue(glucose, Result("5.1"))
        val lipids = Attribute(2, "lipids")
        val lipidValue = AttributeWithValue(lipids, Result("5.2"))
        val case = createViewableCase(CaseId(99, "Case1"), listOf(glucoseValue, lipidValue))
        val haemoglobin = Attribute(3, "haemoglobin")
        val sodium = Attribute(4, "sodium")
        val allAttributes = setOf(glucose, lipids, haemoglobin, sodium)

        // When
        val systemPrompt = KBChatService.systemPrompt(case, allAttributes = allAttributes)

        // Then the prompt contains the attributes that are not on the case
        systemPrompt shouldContain haemoglobin.name
        systemPrompt shouldContain sodium.name
    }

    @Test
    fun `the prompt for a case names the open knowledge base and the available ones`() {
        // Given
        val case = createCaseWithInterpretation("Test Case")

        // When
        val systemPrompt =
            KBChatService.systemPrompt(case, kbName = "Thyroids", kbNames = listOf("Glucose", "Thyroids"))

        // Then
        systemPrompt shouldNotContain "{{"
        systemPrompt shouldContain "Thyroids"
        systemPrompt shouldContain "Glucose, Thyroids"
    }

    @Test
    fun `the prompt with no case has the general sections but none about the case`() {
        // When
        val systemPrompt = KBChatService.systemPrompt(null, kbName = "Thyroids", kbNames = listOf("Thyroids"))

        // Then
        systemPrompt shouldNotContain "{{"
        systemPrompt shouldContain "# Context"
        systemPrompt shouldContain "# Interactions"
        systemPrompt shouldContain "## JSON formatting guidelines"
        systemPrompt shouldContain "# Listing your capabilities"
        systemPrompt shouldNotContain "# Defining the change to the report"
        systemPrompt shouldNotContain "# Example interactions with the user"
    }

    @Test
    fun `the prompt with no knowledge base says so`() {
        // When
        val systemPrompt = KBChatService.systemPrompt(null, kbName = null, kbNames = emptyList())

        // Then
        systemPrompt shouldNotContain "{{"
        systemPrompt shouldContain KBChatService.NO_KB_NAME
    }

    @Test
    fun `sections are chosen by whether there is a case`() {
        // When / Then
        KBChatService.mainSectionsFor(hasCase = false) shouldBe KBChatService.caseLessSections
        KBChatService.mainSectionsFor(hasCase = true) shouldBe KBChatService.systemPromptMainSections
        KBChatService.systemPromptMainSections shouldContainAll KBChatService.caseLessSections
    }
}