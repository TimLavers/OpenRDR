package io.rippledown.caseview

import io.kotest.matchers.shouldBe
import io.rippledown.model.Attribute
import kotlinx.coroutines.test.TestResult
import proxy.findById
import react.FC
import react.dom.test.runReactTest
import kotlin.test.Test

fun attributeCellId(attribute: Attribute) = "attribute_name_cell_${attribute.name}"

class AttributeCellTest {

    @Test
    fun creation(): TestResult {
        val tsh = Attribute(1, "TSH")
        val fc = FC {
            AttributeCell {
                attribute = tsh
            }
        }
        return runReactTest(fc) { container ->
            with(container) {
                val cell = findById(attributeCellId(tsh))
                cell.textContent shouldBe tsh.name
                cell.getAttribute("draggable") shouldBe "true"
            }
        }
    }
}
