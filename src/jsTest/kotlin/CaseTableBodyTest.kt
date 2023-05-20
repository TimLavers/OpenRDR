import io.kotest.matchers.shouldBe
import io.rippledown.model.Attribute
import io.rippledown.model.RDRCaseBuilder
import io.rippledown.model.caseview.CaseViewProperties
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.defaultDate
import kotlinx.coroutines.test.runTest
import proxy.findAllById
import react.VFC
import react.dom.createRootFor
import kotlin.test.Test

class CaseTableBodyTest {

    @Test
    fun attributeOrdering() = runTest {
        val builder1 = RDRCaseBuilder()
        val tsh = Attribute("TSH", 1)
        val ft4 = Attribute("FT4", 2)
        val abc = Attribute("ABC", 3)
        val xyz = Attribute("XYZ", 4)
        builder1.addValue(ft4, defaultDate, "12.8")
        builder1.addValue(abc, defaultDate, "12.9")
        builder1.addValue(xyz, defaultDate, "1.9")
        builder1.addValue(tsh, defaultDate, "2.37")
        val case1 = builder1.build("Case1")
        val properties = CaseViewProperties(listOf(tsh, ft4, abc, xyz))
        val viewableCase = ViewableCase(case1, properties)

        val vfc = VFC {
            CaseTableBody {
                case = viewableCase
            }
        }
        with(createRootFor(vfc)) {
            val rows = findAllById("case_table_row_")
            rows.length shouldBe 4
            rows[0].children[0].textContent shouldBe tsh.name
            rows[1].children[0].textContent shouldBe ft4.name
            rows[2].children[0].textContent shouldBe abc.name
            rows[3].children[0].textContent shouldBe xyz.name

            rows[0].children[1].textContent shouldBe "2.37"
            rows[1].children[1].textContent shouldBe "12.8"
            rows[2].children[1].textContent shouldBe "12.9"
            rows[3].children[1].textContent shouldBe "1.9"
        }
    }
}


