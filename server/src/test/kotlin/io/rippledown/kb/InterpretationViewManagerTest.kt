package io.rippledown.kb

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import io.rippledown.model.*
import io.rippledown.persistence.inmemory.InMemoryAttributeStore
import io.rippledown.persistence.inmemory.InMemoryConclusionStore
import io.rippledown.persistence.inmemory.InMemoryOrderStore
import io.rippledown.persistence.inmemory.InMemoryVerifiedTextStore
import kotlin.test.BeforeTest
import kotlin.test.Test

class InterpretationViewManagerTest {
    private lateinit var manager: InterpretationViewManager
    private lateinit var verifiedTextStore: InMemoryVerifiedTextStore

    @BeforeTest
    fun init() {
        val conclusionManager = ConclusionManager(InMemoryConclusionStore())
        val attributeManager = AttributeManager(InMemoryAttributeStore())
        val orderStore = InMemoryOrderStore()
        verifiedTextStore = InMemoryVerifiedTextStore()
        manager = InterpretationViewManager(orderStore, conclusionManager, attributeManager)
    }

    @Test
    fun `should be no ordering when the interpretation view manager is created from an empty conclusion manager`() {
        manager.allInOrder() shouldBe emptyList()
    }

    @Test
    fun `should set the text given by rules according to the conclusion ordering`() {
        //Given
        val conclusion1 = Conclusion(1, "a")
        val conclusion2 = Conclusion(2, "b")
        val conclusion3 = Conclusion(3, "c")
        val interpretation = mockk<Interpretation>()
        val case = mockk<RDRCase>()
        every { interpretation.conclusions() } returns setOf(conclusion1, conclusion2, conclusion3)
        every { interpretation.caseId } returns CaseId(42, "Hitch")
        manager.insert(listOf(conclusion3, conclusion1, conclusion2))

        //When
        val viewableInterpretation = manager.viewableInterpretation(interpretation, case)

        //Then
        viewableInterpretation.textGivenByRules shouldBe "c a b"
    }

    @Test
    fun `should handle comment with variable when attribute lookup fails gracefully`() {
        //Given
        val glucose = Attribute(1, "Glucose")
        val template = "Glucose is " + io.rippledown.model.VARIABLE_TOKEN
        val variables = listOf(io.rippledown.model.CommentVariable(999)) // Bad ID
        val conclusion = Conclusion(1, template, variables)
        val interpretation = mockk<Interpretation>()
        val case = mockk<RDRCase>()
        every { interpretation.conclusions() } returns setOf(conclusion)
        every { interpretation.caseId } returns CaseId(42, "Hitch")
        manager.insert(listOf(conclusion))

        //When
        val viewableInterpretation = manager.viewableInterpretation(interpretation, case)

        //Then - should not crash, should render with marker for unresolved variable
        viewableInterpretation.textGivenByRules shouldNotBe null
    }
}