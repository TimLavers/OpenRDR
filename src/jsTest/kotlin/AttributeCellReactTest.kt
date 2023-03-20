import io.kotest.matchers.shouldBe
import io.rippledown.model.Attribute
import js.core.get
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.runTest
import mysticfall.checkContainer
import react.VFC
import kotlin.test.Test

class AttributeCellReactTest {
    val tsh = Attribute("TSH", 34)

    val ACWrapper = VFC {
        AttributeCell {
            attribute = tsh
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun attributeShouldBeDraggable(): TestResult = runTest {
        checkContainer(ACWrapper) { container ->
            val element = container.getElementsByTagName("td")[0]
            element.id shouldBe attributeCellId(tsh)
            element.textContent shouldBe tsh.name
            element.getAttribute("draggable") shouldBe "true"
        }
    }
}
