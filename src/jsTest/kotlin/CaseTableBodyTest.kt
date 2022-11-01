import io.kotest.matchers.shouldBe
import io.rippledown.model.*
import io.rippledown.model.caseview.CaseViewProperties
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.rule.RuleSummary
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mocks.defaultMock
import mysticfall.ReactTestSupport
import org.w3c.dom.HTMLTableCellElement
import proxy.*
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CaseTableBodyTest : ReactTestSupport {

    @Test
    fun attributeOrdering() {
        val builder1 = RDRCaseBuilder()
        builder1.addValue("TSH", defaultDate, "2.37")
        builder1.addValue("FT4", defaultDate, "12.8")
        val case1 = builder1.build("Case1")
        val properties = CaseViewProperties(emptyMap())
        val viewableCase = ViewableCase(case1, properties)

        val renderer = render {
            CaseTableBody {
                attrs.case = viewableCase
            }
        }
        val rows = renderer.root.findAllByType("tr")
        println("rows: ${rows.size}")
        val blah = rows[0].children.map { c -> c.props.asDynamic()["className"] }
        println("blah: $blah")

        val attributeCells = renderer.root.findAll{it.props.asDynamic()["id"] != null && (it.props.asDynamic()["id"] as String).startsWith("attribute_name_cell_") }
        println("atts ${attributeCells.size}")
        println("atts ${attributeCells[0].props.asDynamic()["children"].unsafeCast<String>()}")
//        renderer.printJSON()
        println("row 0 ${rows[0].children.size}")
//        println("row 0 child 0 ${rows[0].children[0].type}")
//        println("row 0 child 1 ${rows[0].children[1].type}")
//        rows[0].findAllByType("td")[0].text() shouldBe "Attribute"
    }
}


