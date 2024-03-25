package io.rippledown.caseview

import androidx.compose.material.Button
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import io.mockk.mockk
import io.rippledown.casecontrol.requireValueForAttribute
import io.rippledown.casecontrol.waitForCaseToBeShowing
import io.rippledown.model.Attribute
import io.rippledown.model.RDRCaseBuilder
import io.rippledown.model.caseview.CaseViewProperties
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.defaultDate
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
private const val buttonId = "buttonId"

class CaseViewUpdateTest {
    @get:Rule
    var composeTestRule = createComposeRule()

    private lateinit var caseViewHandler: CaseViewHandler

    @Before
    fun setUp() {
        caseViewHandler = mockk<CaseViewHandler>(relaxed = true)
    }

    @Test
    fun `should recompose case view if the case changes`() {
        val tsh = Attribute(1, "TSH")
        val clinicalNotes = Attribute(2, "Clinical notes")

        val bondiCase = with(RDRCaseBuilder()) {
            addValue(tsh, defaultDate, "2.37")
            addValue(clinicalNotes, defaultDate, "Lethargy")
            build("Bondi")
        }
        val malabarCase = with(RDRCaseBuilder()) {
            addValue(tsh, defaultDate, "2.38")
            addValue(clinicalNotes, defaultDate, "Lethargy")
            build("Malabar")
        }
        val properties = CaseViewProperties(listOf(tsh, clinicalNotes))
        val viewableBondiCase = ViewableCase(bondiCase, properties)
        val viewableMalabarCase = ViewableCase(malabarCase, properties)

        with(composeTestRule) {
            setContent {
                CaseViewWithButton(viewableBondiCase, viewableMalabarCase)
            }

            //Given
            val bondi = viewableBondiCase.name
            waitForCaseToBeShowing(bondi)
            requireValueForAttribute(bondi, tsh.name, "2.37")
            requireValueForAttribute(bondi, clinicalNotes.name, "Lethargy")

            //When update the case
            onNodeWithTag(buttonId).performClick()

            //Then
            val malabar = viewableMalabarCase.name
            waitForCaseToBeShowing(malabar)
            requireValueForAttribute(malabar, tsh.name, "2.38")
            requireValueForAttribute(malabar, clinicalNotes.name, "Lethargy")
        }
    }

    @Test
    fun `should call handler when edited`() {
        //todo
    }
}

@Composable
fun CaseViewWithButton(initialCase: ViewableCase, changedCase: ViewableCase) {

    var currentCase by remember { mutableStateOf(initialCase) }
    val handler = mockk<CaseViewHandler>()

    CaseView(currentCase, handler)

    Button(
        onClick = {
            currentCase = changedCase
        },
        modifier = Modifier.testTag(buttonId)
    ) {}
}
