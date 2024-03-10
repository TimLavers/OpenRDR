package io.rippledown.appbar

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.rippledown.model.KBInfo
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import javax.swing.SwingUtilities.invokeAndWait
import kotlin.test.Test

class KBControlTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val lipidsInfo = KBInfo("12345_id", "Lipids")
    private lateinit var handler: KBControlHandler

    @Before
    fun setup() {
        handler = mockk<KBControlHandler>(relaxed = true)
        every { handler.kbList } returns { emptyList() }
    }

    @Test
    fun `should display the KB name`() {
        with(composeTestRule) {
            setContent {
                KBControl(lipidsInfo, handler)
            }
            assertKbNameIs(lipidsInfo.name)
        }
    }

    @Test
    fun `should show dropdown of all available KBs if there is no current KB`() = runTest {
        val glucose = "Glucose"
        val glucoseInfo = KBInfo(glucose)
        every { handler.kbList } returns { listOf(lipidsInfo, glucoseInfo) }
        with(composeTestRule) {
            setContent {
                KBControl(null, handler)
            }
            //Given
            assertCreateKbButtonIsNotShowing()
            assertKbNameIs("")

            //when
            clickDropdown()

            //Then
            assertCreateKbButtonIsShowing()
            assertDropdownItemsContain(lipidsInfo.name, glucose)

        }
    }

    @Test
    fun `should not include in the dropdown the current KB`() = runTest {
        val glucose = "Glucose"
        val glucoseInfo = KBInfo(glucose)
        every { handler.kbList } returns { listOf(lipidsInfo, glucoseInfo) }
        with(composeTestRule) {
            setContent {
                KBControl(glucoseInfo, handler)
            }
            //Given
            assertCreateKbButtonIsNotShowing()
            assertKbNameIs(glucose)

            //when
            clickDropdown()

            //Then
            assertCreateKbButtonIsShowing()
            assertDropdownItemsContain(lipidsInfo.name)

        }
    }

    @Test
    fun `should create KB`() = runTest {
        val glucose = "Glucose"
        val glucoseInfo = KBInfo(glucose)
        every { handler.kbList } returns { listOf(lipidsInfo, glucoseInfo) }
        with(composeTestRule) {
            setContent {
                KBControl(glucoseInfo, handler)
            }
            //Given
            assertCreateKbButtonIsNotShowing()
            assertKbNameIs(glucose)
            clickDropdown()
            assertCreateKbButtonIsShowing()

            //When
            clickCreateKbButton()
            assertOkButtonIsNotEnabled()
            enterKBName(lipidsInfo.name)
            requireEnteredKBName(lipidsInfo.name)
            invokeAndWait { clickCreateButton() }

            //Then
            verify { handler.createKB(lipidsInfo.name) }
        }
    }
}

fun main() {
    val lipidInfo = KBInfo("Lipids")
    val glucoseInfo = KBInfo("Glucose")
    val handler = mockk<KBControlHandler>(relaxed = true)
    every { handler.kbList } returns { listOf(lipidInfo, glucoseInfo) }

    application {
        Window(
            onCloseRequest = ::exitApplication
        ) {
            KBControl(lipidInfo, handler)
        }
    }
}