package io.rippledown.caseview

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import io.mockk.mockk
import io.rippledown.model.Attribute
import io.rippledown.model.RDRCaseBuilder
import io.rippledown.model.caseview.CaseViewProperties
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.utils.defaultDate
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class CaseViewTest {
    @get:Rule
    var composeTestRule = createComposeRule()

    private lateinit var caseViewHandler: CaseViewHandler

    @Before
    fun setUp() {
        caseViewHandler = mockk<CaseViewHandler>()
    }

    @Test
    fun show() {
        val builder1 = RDRCaseBuilder()
        val tsh = Attribute(1, "TSH")
        val ft4 = Attribute(2, "FT4")
        builder1.addValue(ft4, defaultDate, "12.8")
        builder1.addValue(tsh, defaultDate, "2.37")
        val case1 = builder1.build("Case1")
        val properties = CaseViewProperties(listOf(tsh, ft4))
        val viewableCase = ViewableCase(case1, properties)

        with(composeTestRule) {
            setContent {
                CaseView(viewableCase, caseViewHandler)
            }
            waitUntilExactlyOneExists(hasText(case1.name))
            waitUntilExactlyOneExists(hasText(tsh.name))
            waitUntilExactlyOneExists(hasText(resultText(viewableCase.case.getLatest(tsh)!!)))
        }
    }

    @Test
    fun `should call handler when edited`() {
        //todo
    }
}