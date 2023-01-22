import io.rippledown.model.Conclusion
import io.rippledown.model.Interpretation
import io.rippledown.model.rule.RuleSummary
import kotlinx.browser.document
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mocks.config
import mocks.mock
import mysticfall.ReactTestSupport
import mysticfall.TestRenderer
import proxy.*
import react.create
import react.dom.client.createRoot
import web.dom.Element
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ConclusionsViewTest : ReactTestSupport {

    @Test
    fun shouldShowOneConclusion() = runTest {
        val text = "Go to Bondi now!"
        val interpretation = Interpretation().apply {
            add(RuleSummary(conclusion = Conclusion(text)))
        }
        lateinit var renderer: TestRenderer
        renderer = render {
            ConclusionsView {
                this.interpretation = interpretation
            }
        }
        renderer.printJSON()

//        renderer.requireInterpretation(text)

    }

    @Test
    fun mainTest() = runTest {
        debug("starting test")
        val text = "Go to Bondi now!"
        val interpretation = Interpretation().apply {
            (1..10).forEach {
                add(RuleSummary(conclusion = Conclusion("$it $text")))
            }
        }
        val config = config {
        }
        val ui = ConclusionsView.create {
            api  = Api(mock(config))
            scope = this@runTest
            this.interpretation = interpretation
        }
        proxy.debug("document ${JSON.stringify(document)}")
        val root = document.getElementById("root")
        root?.let { container ->
            createRoot(container.unsafeCast<Element>()).render(ui)
        }
        debug("root ${JSON.stringify(root)}")
        debug("waiting")
        waitForEvents(100_000)
    }
}