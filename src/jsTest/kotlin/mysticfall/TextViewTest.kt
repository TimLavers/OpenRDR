package mysticfall

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mui.material.TextField
import proxy.findById
import react.*
import react.dom.test.runReactTest
import kotlin.test.Test


@OptIn(ExperimentalCoroutinesApi::class)
class TextViewTest {
    val content = "Some text to show"
    val textViewId = "TextView_id"

    val ComponentWithTextField = VFC {
        TextField {
            defaultValue = content
            multiline = true
            id = textViewId
        }
    }

    @Test
    fun shouldRenderMuiMultilineTextField() = runReactTest(ComponentWithTextField) { container ->
        val textViewElement = container.findById(textViewId)
        textViewElement.textContent shouldBe content
    }
}
