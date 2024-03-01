package io.rippledown.appbar

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.kotest.matchers.shouldBe
import io.rippledown.constants.kb.KB_CONTROL_DESCRIPTION
import io.rippledown.constants.kb.KB_CONTROL_ID
import io.rippledown.constants.main.CREATE_KB_ITEM_ID
import io.rippledown.constants.main.CREATE_KB_TEXT
import io.rippledown.constants.main.MAIN_HEADING
import io.rippledown.constants.main.MAIN_HEADING_ID
import io.rippledown.main.Handler
import io.rippledown.main.handlerImpl
import io.rippledown.mocks.engineConfig
import io.rippledown.model.KBInfo
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import javax.swing.SwingUtilities
import kotlin.test.Test

class KBControlTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var uiKbControlOperator: KbControlOperator

    @Before
    fun setup() {
        composeTestRule.setContent {
            ApplicationBar(object : Handler by handlerImpl, AppBarHandler {
                override var isRuleSessionInProgress = false
            })
        }
        uiKbControlOperator = KbControlOperator(composeTestRule)
    }

    @Test
    fun semantics() {
        composeTestRule.setContent {
//            ApplicationBar(object : Handler by handlerImpl, AppBarHandler {
//                override var isRuleSessionInProgress = false
//            })
            TopAppBar(
                modifier = Modifier.semantics {
                    contentDescription = "TopAppBar"
                }
            ) {
                Row {

                    Text(
                        text = "AAA",
                        modifier = Modifier
                            .testTag("TT_AAA")
                            .semantics {
                                contentDescription = "CD_AAA"
                            }
                    )
                    Text(
                        text = "BBB",
                        modifier = Modifier
//                            .testTag("TT_BBB")
                            .semantics {
                                contentDescription = "CD_BBB"
                            }
                    )
                    Text(
                        text = "CCC",
//                        modifier = Modifier
//                            .testTag("TT_CCC")
//                            .semantics {
//                                contentDescription = "CD_CCC"
//                            }
                    )

                }

            }
        }
        composeTestRule.onNodeWithContentDescription("TopAppBar").printToLog("NODE")
    }

    @Test
    fun `should select default project`() {
        uiKbControlOperator.assertKbNameIs(engineConfig.defaultKB.name)
    }

    @Test
    fun `create KB`() = runTest {
        val newKbName = "Lipids"
        engineConfig.returnKBInfo = KBInfo("12345_id", newKbName)
        uiKbControlOperator.assertCreateKbButtonIsNotShowing()
        uiKbControlOperator.clickControl()

        uiKbControlOperator.assertCreateKbButtonIsShowing()
        val uiCreateKB = uiKbControlOperator.clickCreateKbButton()
        uiCreateKB.assertOkButtonIsNotEnabled()
        SwingUtilities.invokeAndWait{
            uiCreateKB.setNameAndClickCreate(newKbName)
        }
        engineConfig.newKbName shouldBe newKbName
    }
}

@OptIn(ExperimentalTestApi::class)
class KbControlOperator(private val composeTestRule: ComposeContentTestRule) {
    init {
        composeTestRule.waitUntilExactlyOneExists(hasTestTag(KB_CONTROL_ID))
    }

    fun assertKbNameIs(expected: String) {
        composeTestRule.waitUntilExactlyOneExists(hasText(expected))
        val onNodeWithTag = composeTestRule.onNodeWithTag(KB_CONTROL_ID)
        onNodeWithTag.assertTextEquals(expected)
    }

    fun clickControl() = composeTestRule.onNodeWithTag(KB_CONTROL_ID).performClick()

    fun assertCreateKbButtonIsNotShowing() = composeTestRule.onAllNodesWithText(CREATE_KB_TEXT).assertCountEquals(0)

    fun assertCreateKbButtonIsShowing() {
        composeTestRule.waitUntilExactlyOneExists(hasText(CREATE_KB_TEXT))
        composeTestRule.onNodeWithText(CREATE_KB_TEXT).assertIsEnabled()
    }

    fun clickCreateKbButton(): CreateKbOperator {
        composeTestRule.onNodeWithTag(CREATE_KB_ITEM_ID).performClick()
        return CreateKbOperator(composeTestRule)
    }
}
