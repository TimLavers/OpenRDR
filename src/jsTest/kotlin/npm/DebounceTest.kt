package npm

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.runTest
import mui.material.Button
import mui.material.TextField
import proxy.findById
import proxy.waitForEvents
import react.FC
import react.Props
import react.dom.createRootFor
import react.dom.onChange
import react.dom.test.Simulate
import react.dom.test.act
import react.useState
import web.events.EventInit
import web.html.HTMLButtonElement
import web.html.HTMLDivElement
import web.html.HTMLElement
import kotlin.test.Test

class DebounceTest {
    val waitMillis = 100L

    @Test
    fun shouldDebounceMouseClicks(): TestResult {
        return runTest {

            val DebouncedButton = FC<Props> {
                var clicks by useState(0)

                fun handleClickEvent(): MouseEventAlias {
                    return {
                        ++clicks
                    }
                }

                fun debounceFunction(): MouseEventAlias {
                    return debounce(handleClickEvent(), waitMillis)
                }

                Button {
                    id = "button"
                    onClick = debounceFunction()

                    +"Clicked $clicks times"
                }
            }
            val vfc = FC {
                DebouncedButton {
                }
            }
            val container = createRootFor(vfc)
            with(container) {
                requireNumberOfClicks(0)
                clickButton()
                clickButton()
                clickButton()
                requireNumberOfClicks(0) // should not have incremented yet
                waitForEvents(waitMillis * 2)
                requireNumberOfClicks(1)
                clickButton()
                clickButton()
                clickButton()
                requireNumberOfClicks(1) // should not have incremented yet
                waitForEvents(waitMillis * 2)
                requireNumberOfClicks(2)
            }
        }
    }


    @Test
    fun shouldDebounceTextViewInput() = runTest {

        val DebouncedTextField = FC<Props> {
            var text by useState("")

            fun handleFormEvent(): FormEventAlias {
                return {
                    text = it.target.asDynamic().value
                }
            }

            fun debounceFunction(): FormEventAlias {
                return debounce(handleFormEvent(), waitMillis)
            }

            TextField {
                id = "text-field"
                multiline = true
                defaultValue = text
                onChange = debounceFunction()
                }
            }
            val vfc = FC {
                DebouncedTextField {
                }
            }
            val container = createRootFor(vfc)
            with(container) {
                requireText("")
                enterText("A")
                enterText("B")
                enterText("C")
                requireText("") // should not have updated yet
                waitForEvents(waitMillis * 2)
                requireText("C")
                enterText("E")
                enterText("F")
                requireText("C") // should not have updated yet
                waitForEvents(waitMillis * 2)
                requireText("F")
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

    private fun HTMLElement.requireText(text: String) {
        findById("text-field").textContent shouldBe text
    }

    private suspend fun HTMLElement.enterText(text: String) {
        val jsName = kotlin.js.json("value" to text)
        val jsEvent = kotlin.js.json("target" to jsName) as EventInit
        val element = findById("text-field")
        act {
            Simulate.change(element, jsEvent)
        }
    }

typealias MouseEventAlias = (react.dom.events.MouseEvent<HTMLButtonElement, *>) -> Unit
typealias FormEventAlias = (react.dom.events.FormEvent<HTMLDivElement>) -> Unit