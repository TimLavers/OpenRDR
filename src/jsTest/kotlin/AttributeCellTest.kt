import io.kotest.matchers.shouldBe
import io.rippledown.model.Attribute
import kotlinx.coroutines.test.runTest
import proxy.findById
import react.VFC
import react.dom.createRootFor
import kotlin.test.Test

fun attributeCellId(attribute: Attribute) = "attribute_name_cell_${attribute.name}"

class AttributeCellTest {

    @Test
    fun creation() = runTest {
        val tsh = Attribute("TSH")
        val vfc = VFC {
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
