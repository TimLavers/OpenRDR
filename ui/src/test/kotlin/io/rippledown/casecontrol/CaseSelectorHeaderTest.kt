package io.rippledown.casecontrol

import androidx.compose.ui.test.junit4.createComposeRule
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import kotlin.test.Test

class CaseSelectorHeaderTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    lateinit var handler: CaseSelectorHandler

    @Before
    fun setUp() {
        handler = mockk<CaseSelectorHandler>()
    }

    @Test
    fun `should show plural cases label if more than one case`() = runTest {
        with(composeTestRule) {
            setContent {
                CaseSelectorHeader(2)
            }
            requireCaseSelectorLabelToBe("2 cases")
        }
    }

    @Test
    fun `should show singular case label if only one case`() = runTest {
        with(composeTestRule) {
            setContent {
                CaseSelectorHeader(1)
            }
            requireCaseSelectorLabelToBe("1 case")
        }
    }

    @Test
    fun `should show label for no cases`() = runTest {
        with(composeTestRule) {
            setContent {
                CaseSelectorHeader(0)
            }
            requireCaseSelectorLabelToBe("no cases")
        }
    }
}