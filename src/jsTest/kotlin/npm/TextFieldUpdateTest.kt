package npm

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.TestResult
import mui.material.Button
import mui.material.TextField
import proxy.findById
import react.FC
import react.dom.test.act
import react.dom.test.runReactTest
import react.useState
import kotlin.test.Test

class TextFieldUpdateTest {

    @Test
    fun shouldShowVerifiedTextThatHasBeenUpdated(): TestResult {
        val bondiText = "Go to Bondi"
        val manlyText = "Go to Manly"
        val buttonId = "button_id"
        val textFieldId = "text_field_id"
        val vfc = FC {
            var latestText by useState(bondiText)

            Button {
                id = buttonId
                onClick = {
                    latestText = manlyText
                }
            }

            TextField {
                id = textFieldId
                multiline = true
                defaultValue = latestText
            }
        }
        return runReactTest(vfc) { container ->
            with(container) {
                findById(textFieldId).textContent shouldBe bondiText
                act { findById(buttonId).click() }
                findById(textFieldId).textContent shouldBe manlyText
            }
        }
    }
}