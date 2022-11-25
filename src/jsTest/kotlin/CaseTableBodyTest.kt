import io.kotest.matchers.shouldBe
import io.rippledown.model.Attribute
import io.rippledown.model.RDRCaseBuilder
import io.rippledown.model.caseview.CaseViewProperties
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.defaultDate
import mysticfall.ReactTestSupport
import proxy.text
import kotlin.test.Test

class CaseTableBodyTest : ReactTestSupport {

    @Test
    fun attributeOrdering() {
        val builder1 = RDRCaseBuilder()
        val tsh = Attribute("TSH")
        val ft4 = Attribute("FT4")
        val abc = Attribute("ABC")
        val xyz = Attribute("XYZ")
        builder1.addValue(ft4.name, defaultDate, "12.8")
        builder1.addValue(abc.name, defaultDate, "12.9")
        builder1.addValue(xyz.name, defaultDate, "1.9")
        builder1.addValue(tsh.name, defaultDate, "2.37")
        val case1 = builder1.build("Case1")
        val properties = CaseViewProperties(listOf(tsh, ft4, abc, xyz))
        val viewableCase = ViewableCase(case1, properties)

        val renderer = render {
            CaseTableBody {
                case = viewableCase
            }
        }
        val rows = renderer.root.findAllByType("tr")
        rows.size shouldBe 4

        val attributeCells = renderer.root.findAll{
            val id = it.props.asDynamic()["id"]
            id != null && (id as String).startsWith("attribute_name_cell_")
        }
        attributeCells[0].text() shouldBe tsh.name
        attributeCells[1].text() shouldBe ft4.name
        attributeCells[2].text() shouldBe abc.name
        attributeCells[3].text() shouldBe xyz.name
    }
}


