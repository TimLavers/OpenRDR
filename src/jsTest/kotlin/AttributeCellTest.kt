import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.rippledown.model.Attribute
import mysticfall.ReactTestSupport
import react.dom.html.ReactHTML
import kotlin.test.Test

class AttributeCellTest : ReactTestSupport {

    @Test
    fun creation() {
        val tsh = Attribute("TSH")
        val renderer = render {
            AttributeCell {
                attrs.attribute = tsh
            }
        }
        val cells = renderer.root.findAllByType(AttributeCell)
        cells[0].props.attribute shouldBe tsh
        val byId = renderer.root.findAllByType(ReactHTML.td.toString())
            .first {
                it.props.asDynamic()["id"] == "attribute_name_cell_${tsh.name}"
            }
        byId shouldNotBe null

        val byText = renderer.root.findAllByType(ReactHTML.td.toString())
            .first {
                it.props.asDynamic()["children"] == tsh.name
            }
        byText shouldNotBe null
    }
}
