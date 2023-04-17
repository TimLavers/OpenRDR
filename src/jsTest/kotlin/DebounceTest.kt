import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.runTest
import mui.material.Button
import npm.debounce
import proxy.findById
import proxy.waitForEvents
import react.FC
import react.Props
import react.VFC
import react.dom.createRootFor
import react.dom.test.Simulate
import react.dom.test.act
import react.useState
import web.html.HTMLElement
import kotlin.test.Test

class DebounceTest {
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun shouldDebounce(): TestResult {
        val waitMillis = 100
        return runTest {

            val DebouncedButton = FC<Props> {
                var clicks by useState(0)

                fun debouncedMouseClick() = debounce({
                    ++clicks
                    debug("*** Clicked $clicks times ***")
                }, waitMillis)


                Button {
                    +"Clicked $clicks times"
                    onClick = { _ ->
                        debug("called debouncedMouseClick")
                        debouncedMouseClick()
                        debug("return debouncedMouseClick")
                    }
                    id = "button"
                }
            }
            val vfc = VFC {
                DebouncedButton {
                }
            }
            val container = createRootFor(vfc)
            with(container) {
                requireNumberOfClicks(0)
                clickButton()
                clickButton()
                clickButton()
                requireNumberOfClicks(0)
                waitForEvents(waitMillis.toLong() * 4)
                requireNumberOfClicks(3)
                clickButton()
                clickButton()
                clickButton()
                requireNumberOfClicks(3)
                waitForEvents(waitMillis.toLong() * 4)
                requireNumberOfClicks(6)
            }
        }
    }
}


private suspend fun HTMLElement.clickButton() {
    val element = findById("button")
    act {
        Simulate.click(element)
    }
}

private fun HTMLElement.requireNumberOfClicks(i: Int) {
    findById("button").textContent shouldBe "Clicked $i times"
}
