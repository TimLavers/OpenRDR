package mysticfall

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mui.base.TextareaAutosize
import proxy.findById
import react.*
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
    fun shouldAssignIdsAndLabels() = runTest {
        checkContainer(Wrapper) { container ->
            val byId = container.findById("id_1")
            byId.textContent shouldBe "go to Bondi Beach"
        }
    }
}

