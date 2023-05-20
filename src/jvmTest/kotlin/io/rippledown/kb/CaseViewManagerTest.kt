package io.rippledown.kb

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.ints.shouldBeLessThan
import io.kotest.matchers.shouldBe
import io.rippledown.model.Attribute
import io.rippledown.model.RDRCase
import io.rippledown.model.RDRCaseBuilder
import io.rippledown.model.TestResult
import io.rippledown.model.caseview.ViewableCase
import java.time.Instant
import kotlin.test.BeforeTest
import kotlin.test.Test

class CaseViewManagerTest {

    val a1 = Attribute("A1")
    val a2 = Attribute("A2")
    val a3 = Attribute("A3")
    val a4 = Attribute("A4")
    val a5 = Attribute("A5")
    val a6 = Attribute("A6")
    private var manager = CaseViewManager()

    @BeforeTest
    fun setup() {
        manager = CaseViewManager()
    }

    @Test
    fun `attributes in first case are above attributes in second case`() {
        val case1Attributes = listOf(a1, a3, a5)
        val case1 = createCase("Case1", case1Attributes)
        val case2Attributes = listOf(a2, a4, a6)
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
        }.message shouldBe "Unknown attribute: $a2"
    }

    @Test
    fun `unknown target attribute`() {
        manager.getViewableCase(createCase("Case1", listOf(a1)))
        shouldThrow<IllegalStateException>{
            manager.moveJustBelow(a1, a2)
        }.message shouldBe "Unknown attribute: $a2"
    }

    @Test
    fun `moved and target attributes must be distinct`() {
        manager.getViewableCase(createCase("Case1", listOf(a1)))
        shouldThrow<IllegalStateException>{
            manager.moveJustBelow(a1, a1)
        }.message shouldBe "Moved attribute is target attribute, $a1"
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
        manager.setAttributes(listOf( a3, a2, a1))
        val case = createCaseWithAttributesAndShowToManager(listOf(a1, a2, a3))
        case.attributes() shouldBe listOf(a3, a2, a1)
        manager.allAttributesInOrder() shouldBe listOf(a3, a2, a1)
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
        val caseAfter = manager.getViewableCase(case.rdrCase)
        caseAfter.attributes() shouldBe listOf(a6, a5, a4, a3, a2, a1)
    }

    @Test
    fun allAttributesInOrder() {
        createCaseWithAttributesAndShowToManager(listOf(a1))
        createCaseWithAttributesAndShowToManager(listOf(a2))
        createCaseWithAttributesAndShowToManager(listOf(a3))
        createCaseWithAttributesAndShowToManager(listOf(a4))
        manager.allAttributesInOrder() shouldBe listOf(a1, a2, a3, a4)
        manager.moveJustBelow(a4, a1)
        manager.moveJustBelow(a3, a4)
        manager.moveJustBelow(a2, a3)
        manager.moveJustBelow(a1, a2)
        manager.allAttributesInOrder() shouldBe listOf(a4, a3, a2, a1)
    }

    @Test
    fun allAttributesInOrderEmpty() {
        manager.allAttributesInOrder() shouldBe listOf()
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
        return builder.build(name)
    }

}