package io.rippledown.caseview

import io.kotest.matchers.shouldBe
import io.rippledown.model.Attribute
import kotlinx.coroutines.test.runTest
import proxy.findById
import react.FC
import react.dom.createRootFor
import kotlin.test.Test

fun attributeCellId(attribute: Attribute) = "attribute_name_cell_${attribute.name}"

class AttributeCellTest {

    @Test
    fun creation() = runTest {
        val tsh = Attribute(1, "TSH")
        val vfc = FC {
            AttributeCell {
                attribute = tsh
            }
        }
        with(createRootFor(vfc)) {
            val cell = findById(attributeCellId(tsh))
            cell.textContent shouldBe tsh.name
            cell.getAttribute("draggable") shouldBe "true"
        }
    }
}
