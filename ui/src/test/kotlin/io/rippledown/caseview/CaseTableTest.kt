package io.rippledown.caseview

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import io.rippledown.model.*
import io.rippledown.model.caseview.CaseViewProperties
import io.rippledown.model.caseview.ViewableCase
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class CaseTableTest {
    @get:Rule
    var composeTestRule = createComposeRule()

    @Test
    fun show() {
        val builder1 = RDRCaseBuilder()
        val tsh = Attribute(1, "TSH")
        val ft4 = Attribute(2, "FT4")
        val abc = Attribute(3, "ABC")
        val xyz = Attribute(4, "XYZ")
        builder1.addValue(ft4, defaultDate, "12.8")
        builder1.addValue(abc, defaultDate, "12.9")
        builder1.addValue(xyz, defaultDate, "1.9")
        builder1.addValue(tsh, defaultDate, "2.37")
        val case1 = builder1.build("Case1")
        val properties = CaseViewProperties(listOf(tsh, ft4, abc, xyz))
        val viewableCase = ViewableCase(case1, properties)
        val caseTableHandler = object : CaseTableHandler {
            override val viewableCase: ViewableCase
                get() = viewableCase
        }

        composeTestRule.setContent {
            CaseTable( caseTableHandler)
        }
        with(composeTestRule) {
            waitUntilExactlyOneExists(hasText(tsh.name))
            waitUntilExactlyOneExists(hasText(resultText(viewableCase.case.getLatest(tsh)!!)))
        }
    }
}