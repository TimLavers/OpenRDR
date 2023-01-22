import js.core.get
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.rippledown.model.Attribute
import kotlinext.js.asJsObject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mysticfall.ReactTestSupport
import proxy.printJSON
import react.VFC
import react.dom.html.ReactHTML
import react.dom.test.runReactTest
import kotlin.test.Test
import kotlinx.coroutines.test.TestResult

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
