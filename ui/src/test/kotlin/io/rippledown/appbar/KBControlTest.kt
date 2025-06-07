package io.rippledown.appbar

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.rippledown.model.KBInfo
import io.rippledown.sample.SampleKB
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import java.io.File
import javax.swing.SwingUtilities.invokeAndWait
import kotlin.test.Test

class KBControlTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val lipidsInfo = KBInfo("12345_id", "Lipids")
    private lateinit var handler: KBControlHandler

    @Before
    fun setup() {
        handler = mockk<KBControlHandler>()
        every { handler.kbList } returns { emptyList() }
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
            assertCreateKbMenuItemIsNotShowing()

            //when
            clickDropdown()

            //Then
            assertCreateKbMenuItemIsShowing()
            assertDropdownItemsContain(lipidsInfo.name, glucose)
        }
    }

    @Test
    fun `should not include the current KB in the dropdown`() = runTest {
        val glucose = "Glucose"
        val glucoseInfo = KBInfo(glucose)
        every { handler.kbList } returns { listOf(lipidsInfo, glucoseInfo) }
        with(composeTestRule) {
            setContent {
                KBControl(glucoseInfo, handler)
            }
            //Given
            assertCreateKbMenuItemIsNotShowing()

            //when
            clickDropdown()

            //Then
            assertCreateKbMenuItemIsShowing()
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
            assertCreateKbMenuItemIsNotShowing()
            clickDropdown()
            assertCreateKbMenuItemIsShowing()

            //When
            clickCreateKbMenuItem()
            assertOkButtonIsNotEnabled()
            enterKbName(lipidsInfo.name)
            requireEnteredKBName(lipidsInfo.name)
            invokeAndWait { clickCreateButton() }

            //Then
            verify { handler.createKB(lipidsInfo.name) }
        }
    }

    @Test
    fun `should create KB from sample`() = runTest {
        val glucose = "Glucose"
        val glucoseInfo = KBInfo(glucose)
        every { handler.kbList } returns { listOf(lipidsInfo, glucoseInfo) }
        with(composeTestRule) {
            setContent {
                KBControl(glucoseInfo, handler)
            }
            //Given
            assertCreateKbMenuItemIsNotShowing()
            clickDropdown()
            assertCreateKbFromSampleMenuItemIsShowing()

            //When
            clickCreateKbFromSampleMenuItem()
            assertOkButtonIsNotEnabled()
            enterKbName(lipidsInfo.name)
            requireEnteredKBName(lipidsInfo.name)
            invokeAndWait { clickCreateButton() }

            //Then
            verify { handler.createKBFromSample(lipidsInfo.name, SampleKB.TSH) }
        }
    }

    @Test
    fun `should import KB`() = runTest {
        val glucose = "Glucose"
        val resourcesRoot = "src/test/resources/"
        val zip = File("${resourcesRoot}export/Empty.zip")
        val glucoseInfo = KBInfo(glucose)
        every { handler.kbList } returns { listOf(lipidsInfo, glucoseInfo) }
        with(composeTestRule) {
            setContent {
                KBControl(glucoseInfo, handler)
            }
            //Given
            assertImportKbMenuItemIsNotShowing()
            clickDropdown()
            assertImportKbMenuItemIsShowing()

            //When
            clickImportKbMenuItem()
            assertImportButtonIsNotEnabled()
            enterZipFileName(zip.absolutePath)
            invokeAndWait { clickImportButton() }

            //Then
//            verify { handler.importKB(zip) }
        }
    }
}

fun main() {
    val lipidInfo = KBInfo("Lipids")
    val glucoseInfo = KBInfo("Glucose")
    val handler = mockk<KBControlHandler>()
    every { handler.kbList } returns { listOf(lipidInfo, glucoseInfo) }

    application {
        Window(
            onCloseRequest = ::exitApplication
        ) {
            KBControl(lipidInfo, handler)
        }
    }
}