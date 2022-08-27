import io.kotest.matchers.shouldNotBe
import mysticfall.ReactTestSupport
import kotlin.test.Test

class MainTest : ReactTestSupport {

    @Test
    fun caseQueueShouldShowOnTheMainWindow() {
        val renderer = render {
            OpenRDRUI()
        }
        renderer.root.findAllByType(CaseQueue) shouldNotBe null
    }

}
