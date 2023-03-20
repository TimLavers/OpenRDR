import io.kotest.matchers.shouldBe
import io.rippledown.model.Attribute
import mysticfall.ReactTestSupport
import react.dom.html.ReactHTML
import kotlin.test.Test

fun attributeCellId(attribute: Attribute) = "attribute_name_cell_${attribute.name}"

class AttributeCellTest : ReactTestSupport {

    @Test
    fun creation() {
        val tsh = Attribute("TSH", 1)
        val renderer = render {
            AttributeCell {
                attribute = tsh
            }
        }
        val cells = renderer.root.findAllByType(AttributeCell)
        cells[0].props.attribute shouldBe tsh

        val byId = renderer.root.findAllByType(ReactHTML.td.toString()).first()
        byId.props.asDynamic()["id"].unsafeCast<String>() shouldBe attributeCellId(tsh)
        byId.props.asDynamic()["children"].unsafeCast<String>() shouldBe tsh.name
        byId.props.asDynamic()["draggable"].unsafeCast<Boolean>() shouldBe true
    }
}
