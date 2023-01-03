import io.kotest.matchers.shouldBe
import io.rippledown.model.*
import io.rippledown.kb.ID_KB_INFO_HEADING
import io.rippledown.kb.KBInfoPane
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mocks.config
import mocks.mock
import mysticfall.ReactTestSupport
import mysticfall.TestRenderer
import proxy.*
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class KBInfoPaneTest : ReactTestSupport {

    @Test
    fun shouldShowKBName() = runTest{
        lateinit var renderer: TestRenderer
        act {
            renderer = render {
                KBInfoPane {
                    scope = this@runTest
                    api = Api(mock(config{}))
                }
            }
        }
        renderer.findById(ID_KB_INFO_HEADING).text() shouldBe "Knowledge Base: "

        renderer.waitForItemToHaveText(ID_KB_INFO_HEADING, "Knowledge Base: Glucose")
    }
}