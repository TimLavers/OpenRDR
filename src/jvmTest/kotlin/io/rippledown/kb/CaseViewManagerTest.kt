package io.rippledown.kb

import io.kotest.matchers.ints.shouldBeLessThan
import io.rippledown.model.Attribute
import io.rippledown.model.RDRCase
import io.rippledown.model.RDRCaseBuilder
import io.rippledown.model.TestResult
import java.time.Instant
import kotlin.test.Test

class CaseViewManagerTest {

    val a1 = Attribute("A1")
    val a2 = Attribute("A2")
    val a3 = Attribute("A3")
    val a4 = Attribute("A4")
    val a5 = Attribute("A5")
    val a6 = Attribute("A6")

    /*

   -
    - move to top-1
    - move to bottom
    - move up one
    - move down one
    - move onto self
    - move up 2
    - move down 2
    - move up several
    - move down several
     */

    @Test
    fun `attributes in first case are above attributes in second case`() {
        val manager = CaseViewManager()
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
        val manager = CaseViewManager()
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