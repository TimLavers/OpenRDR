package io.rippledown.caseview

import io.kotest.matchers.shouldBe
import io.rippledown.constants.caseview.CASE_TABLE_ROW_PREFIX
import io.rippledown.model.Attribute
import io.rippledown.model.RDRCaseBuilder
import io.rippledown.model.caseview.CaseViewProperties
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.defaultDate
import kotlinx.coroutines.test.runTest
import proxy.findAllById
import react.FC
import react.dom.createRootFor
import kotlin.test.Test

class CaseTableBodyTest {

    @Test
    fun attributeOrdering() = runTest {
        val builder1 = RDRCaseBuilder()
        val tsh = Attribute(1, "TSH")
        val ft4 = Attribute(2, "FT4")
        val abc = Attribute(3, "ABC")
        val xyz = Attribute(4, "XYZ")
        builder1.addValue(ft4, defaultDate, "12.8")
        builder1.addValue(abc, defaultDate, "12.9")
        builder1.addValue(xyz, defaultDate, "1.9")
        builder1.addValue(tsh, defaultDate, "2.37")
        val case1 = builder1.build( "Case1")
        val properties = CaseViewProperties(listOf(tsh, ft4, abc, xyz))
        val viewableCase = ViewableCase(case1, properties)

        val vfc = FC {
            CaseTableBody {
                case = viewableCase
            }
        }
        with(createRootFor(vfc)) {
            val rows = findAllById(CASE_TABLE_ROW_PREFIX)
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


