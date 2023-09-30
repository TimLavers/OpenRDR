package io.rippledown.kb

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.ints.shouldBeLessThan
import io.kotest.matchers.shouldBe
import io.rippledown.kb.OrderedEntityManager.Companion.MOVED_ENTITY_IS_TARGET
import io.rippledown.kb.OrderedEntityManager.Companion.UNKNOWN_ENTITY
import io.rippledown.model.Attribute
import io.rippledown.model.RDRCase
import io.rippledown.model.RDRCaseBuilder
import io.rippledown.model.TestResult
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.persistence.OrderStore
import io.rippledown.persistence.inmemory.InMemoryAttributeStore
import io.rippledown.persistence.inmemory.InMemoryOrderStore
import java.time.Instant
import kotlin.test.BeforeTest
import kotlin.test.Test

class CaseViewManagerTest {
    private lateinit var a1: Attribute
    private lateinit var a2: Attribute
    private lateinit var a3: Attribute
    private lateinit var a4: Attribute
    private lateinit var a5: Attribute
    private lateinit var a6: Attribute
    private lateinit var attributeManager: AttributeManager
    private lateinit var attributeOrderStore: OrderStore
    private lateinit var manager: CaseViewManager

    @BeforeTest
    fun setup() {
        attributeManager = AttributeManager(InMemoryAttributeStore())
        a1 = attributeManager.getOrCreate("A1")
        a2 = attributeManager.getOrCreate("A2")
        a3 = attributeManager.getOrCreate("A3")
        a4 = attributeManager.getOrCreate("A4")
        a5 = attributeManager.getOrCreate("A5")
        a6 = attributeManager.getOrCreate("A6")

        attributeOrderStore = InMemoryOrderStore()
        manager = CaseViewManager(attributeOrderStore, attributeManager)
    }

    @Test
    fun `a case's viewable interpretation should refer to the case's id`() {
        val case = createCase("Case1", listOf())
        manager.getViewableCase(case).viewableInterpretation.caseId() shouldBe case.caseId
    }
    @Test
    fun `should be no ordering when the manager is first created`() {
        manager.allInOrder() shouldBe emptyList()
    }

    @Test
    fun `load from the attribute order store`() {
        attributeOrderStore.store(a4.id, 1)
        attributeOrderStore.store(a3.id, 2)
        attributeOrderStore.store(a2.id, 3)
        attributeOrderStore.store(a1.id, 4)
        attributeOrderStore.store(a5.id, 5)
        attributeOrderStore.store(a6.id, 6)
        manager = CaseViewManager(attributeOrderStore, attributeManager)
        manager.allInOrder() shouldBe listOf(a4, a3, a2, a1, a5, a6)
    }

    @Test
    fun `load from the attribute order store with not all attributes in order store`() {
        attributeOrderStore.store(a4.id, 1)
        attributeOrderStore.store(a3.id, 2)
        manager = CaseViewManager(attributeOrderStore, attributeManager)
        manager.allInOrder()[0] shouldBe a4
        manager.allInOrder()[1] shouldBe a3
    }

    @Test
    fun `attributes in first case are above attributes in second case`() {
        val case1Attributes = listOf(a1, a3, a5)
        val case2Attributes = listOf(a2, a4, a6)
        val case1 = createCase("Case1", case1Attributes)
        val case2 = createCase("Case2", case2Attributes)
        manager.getViewableCase(case1)
        manager.getViewableCase(case2)
        val case3Attributes = listOf(a1, a2, a3, a4, a5, a6)
        val case3 = createCase("Case3", case3Attributes)
        val viewableCase = manager.getViewableCase(case3)
        val attributesOrdered = viewableCase.attributes()
        attributesOrdered.indexOf(a1) shouldBeLessThan attributesOrdered.indexOf(a2)
        attributesOrdered.indexOf(a1) shouldBeLessThan attributesOrdered.indexOf(a4)
        attributesOrdered.indexOf(a1) shouldBeLessThan attributesOrdered.indexOf(a6)
        attributesOrdered.indexOf(a3) shouldBeLessThan attributesOrdered.indexOf(a2)
        attributesOrdered.indexOf(a3) shouldBeLessThan attributesOrdered.indexOf(a4)
        attributesOrdered.indexOf(a3) shouldBeLessThan attributesOrdered.indexOf(a6)
        attributesOrdered.indexOf(a5) shouldBeLessThan attributesOrdered.indexOf(a2)
        attributesOrdered.indexOf(a5) shouldBeLessThan attributesOrdered.indexOf(a4)
        attributesOrdered.indexOf(a5) shouldBeLessThan attributesOrdered.indexOf(a6)
    }

    @Test
    fun `second case shares some attributes with first case`() {
        val case1Attributes = listOf(a1, a3, a5)
        val case1 = createCase("Case1", case1Attributes)
        val case2Attributes = listOf(a2, a3, a4, a5)
        val case2 = createCase("Case2", case2Attributes)
        manager.getViewableCase(case1)
        val viewableCase = manager.getViewableCase(case2)
        val attributesOrdered = viewableCase.attributes()
        attributesOrdered.indexOf(a3) shouldBeLessThan attributesOrdered.indexOf(a2)
        attributesOrdered.indexOf(a3) shouldBeLessThan attributesOrdered.indexOf(a4)
        attributesOrdered.indexOf(a5) shouldBeLessThan attributesOrdered.indexOf(a2)
        attributesOrdered.indexOf(a5) shouldBeLessThan attributesOrdered.indexOf(a4)
    }

    @Test
    fun `move unknown attribute`() {
        manager.getViewableCase(createCase("Case1", listOf(a1)))
        shouldThrow<IllegalStateException>{
            manager.moveJustBelow(a2, a1)
        }.message shouldBe "$UNKNOWN_ENTITY$a2"
    }

    @Test
    fun `unknown target attribute`() {
        manager.getViewableCase(createCase("Case1", listOf(a1)))
        shouldThrow<IllegalStateException>{
            manager.moveJustBelow(a1, a2)
        }.message shouldBe "$UNKNOWN_ENTITY$a2"
    }

    @Test
    fun `moved and target attributes must be distinct`() {
        manager.getViewableCase(createCase("Case1", listOf(a1)))
        shouldThrow<IllegalStateException> {
            manager.moveJustBelow(a1, a1)
        }.message shouldBe "$MOVED_ENTITY_IS_TARGET$a1"
    }

    @Test
    fun moveJustBelow() {
        createCaseWithAttributesAndShowToManager(listOf(a1, a3, a5))
        manager.moveJustBelow(a5, a1)
        val caseAfter = createCaseWithAttributesAndShowToManager(listOf(a1, a3, a5))
        caseAfter.attributes() shouldBe listOf(a1, a5, a3)
    }

    @Test
    fun setAttributes() {
        manager.set(listOf(a3, a2, a1))
        val case = createCaseWithAttributesAndShowToManager(listOf(a1, a2, a3))
        case.attributes() shouldBe listOf(a3, a2, a1)
        manager.allInOrder() shouldBe listOf(a3, a2, a1)
    }

    @Test
    fun moveDownToJustBelow() {
        createCaseWithAttributesAndShowToManager(listOf(a1, a3, a5))
        manager.moveJustBelow(a1, a5)
        val caseAfter = createCaseWithAttributesAndShowToManager(listOf(a1, a3, a5))
        caseAfter.attributes() shouldBe listOf(a3, a5, a1)
    }

    @Test
    fun twoAttributes() {
        val caseBefore = createCaseWithAttributesAndShowToManager(listOf(a1, a2))
        val attributesBefore = caseBefore.attributes()
        manager.moveJustBelow(attributesBefore[0], attributesBefore[1])
        val caseAfter = createCaseWithAttributesAndShowToManager(listOf(a1, a2))
        caseAfter.attributes() shouldBe listOf(attributesBefore[1], attributesBefore[0])
    }

    @Test
    fun orderAllAttributes() {
        createCaseWithAttributesAndShowToManager(listOf(a1))
        createCaseWithAttributesAndShowToManager(listOf(a2))
        createCaseWithAttributesAndShowToManager(listOf(a3))
        createCaseWithAttributesAndShowToManager(listOf(a4))
        createCaseWithAttributesAndShowToManager(listOf(a5))
        createCaseWithAttributesAndShowToManager(listOf(a6))
        val case = createCaseWithAttributesAndShowToManager(listOf(a1, a2, a3, a4, a5, a6))
        case.attributes() shouldBe listOf(a1, a2, a3, a4, a5, a6) // sanity
        manager.moveJustBelow(a6, a1)
        manager.moveJustBelow(a5, a6)
        manager.moveJustBelow(a4, a5)
        manager.moveJustBelow(a3, a4)
        manager.moveJustBelow(a2, a3)
        manager.moveJustBelow(a1, a2)
        val caseAfter = manager.getViewableCase(case.case)
        caseAfter.attributes() shouldBe listOf(a6, a5, a4, a3, a2, a1)
    }

    @Test
    fun newAttribute() {

    }

    @Test
    fun allAttributesInOrder() {
        createCaseWithAttributesAndShowToManager(listOf(a1))
        createCaseWithAttributesAndShowToManager(listOf(a2))
        createCaseWithAttributesAndShowToManager(listOf(a3))
        createCaseWithAttributesAndShowToManager(listOf(a4))
        manager.allInOrder() shouldBe listOf(a1, a2, a3, a4)
        manager.moveJustBelow(a4, a1)
        manager.moveJustBelow(a3, a4)
        manager.moveJustBelow(a2, a3)
        manager.moveJustBelow(a1, a2)
        manager.allInOrder() shouldBe listOf(a4, a3, a2, a1)
    }

    @Test
    fun allAttributesInOrderEmpty() {
        manager.allInOrder() shouldBe listOf()
    }

    private fun createCaseWithAttributesAndShowToManager(attributes: List<Attribute>): ViewableCase {
        val case1 = createCase("Case1", attributes)
        return manager.getViewableCase(case1)
    }

    private fun createCase(name: String, attributes: List<Attribute>): RDRCase {
        val date = Instant.now()
        val builder = RDRCaseBuilder()
        attributes.forEach{
            builder.addResult(it, date.toEpochMilli(), TestResult(it.name + " value") )
        }
        return builder.build( name)
    }
}