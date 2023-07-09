package npm

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import mui.material.Button
import mui.material.TextField
import proxy.findById
import react.FC
import react.dom.createRootFor
import react.dom.test.act
import react.useState
import kotlin.test.Test

class TextFieldUpdateTest {

    @Test
    fun shouldShowVerifiedTextThatHasBeenUpdated() = runTest {
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
        with(createRootFor(vfc)) {
            findById(textFieldId).textContent shouldBe bondiText
            act { findById(buttonId).click() }
            findById(textFieldId).textContent shouldBe manlyText

        }

    }

}