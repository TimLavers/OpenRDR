import io.kotest.matchers.shouldBe
import io.rippledown.model.Attribute
import js.core.get
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestResult
import react.VFC
import react.dom.test.runReactTest
import kotlin.test.Test

class AttributeCellReactTest {
    val tsh = Attribute("TSH")

    val ACWrapper = VFC {
        AttributeCell {
            attribute = tsh
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun initial(): TestResult = runReactTest(ACWrapper) { container ->
        val element = container.getElementsByTagName("td")[0]
        element.id shouldBe attributeCellId(tsh)
        element.textContent shouldBe tsh.name
        element.getAttribute("draggable") shouldBe "true"
    }
}
