import io.kotest.matchers.shouldBe
import io.rippledown.model.CaseId
import io.rippledown.model.RDRCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import mysticfall.ReactTestSupport
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.li
import kotlin.test.Test

class CaseQueueTest : ReactTestSupport {

    @Test
    fun shouldNotShowCaseListByDefault() {
        val component = render(CaseQueue::class)
        component.find("ul") shouldBe null
    }
}


