import io.kotest.matchers.shouldBe
import io.rippledown.model.Attribute
import io.rippledown.model.RDRCaseBuilder
import io.rippledown.model.caseview.CaseViewProperties
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.defaultDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import proxy.findAllById
import react.VFC
import react.dom.createRootFor
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CaseTableBodyTest {

    @Test
    fun attributeOrdering() = runTest {
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


