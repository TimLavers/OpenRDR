package mysticfall

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import js.core.get
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mui.base.TextareaAutosize
import react.*
import react.dom.test.runReactTest
import kotlin.test.Test

class TextareaAutosizeTest {

    val Wrapper = VFC {
        TextareaAutosize {
            id = "id_1"
            defaultValue = "go to Bondi Beach"
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun shouldAssignIdsAndLabels() = runReactTest(Wrapper) { container ->
        val byId = container.querySelectorAll("[id*='id_1']")[0]
        byId.id shouldContain "id_1"
        byId.textContent shouldBe "go to Bondi Beach"
    }
}

