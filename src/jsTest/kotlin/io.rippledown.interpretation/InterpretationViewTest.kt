package io.rippledown.interpretation

import Api
import io.kotest.matchers.shouldBe
import io.rippledown.constants.interpretation.DEBOUNCE_WAIT_PERIOD_MILLIS
import io.rippledown.model.Conclusion
import io.rippledown.model.Interpretation
import io.rippledown.model.rule.RuleSummary
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mocks.config
import mocks.mock
import proxy.enterInterpretation
import proxy.requireInterpretation
import proxy.waitForEvents
import react.VFC
import react.dom.createRootFor
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class InterpretationViewTest {

    @Test
    fun shouldShowInitialInterpretationIfVerifiedTextIsNull() = runTest {
        val text = "Go to Bondi now!"
        val interpretation = Interpretation().apply {
            add(RuleSummary(conclusion = Conclusion(1, text)))
        }
        val vfc = VFC {
            InterpretationView {
                this.interpretation = interpretation
            }
        }
        createRootFor(vfc).requireInterpretation(text)
    }

    @Test
    fun shouldShowVerifiedTextIfNotNull() = runTest {
        val text = "Go to Bondi."
        val verifiedText = "Go to Bondi now!"
        val interpretation = Interpretation(verifiedText = verifiedText).apply {
            add(RuleSummary(conclusion = Conclusion(1, text)))
        }
        val vfc = VFC {
            InterpretationView {
                this.interpretation = interpretation
            }
        }
        createRootFor(vfc).requireInterpretation(verifiedText)
    }

    @Test
    fun shouldShowBlankInterpretation() = runTest {
        val vfc = VFC {
            InterpretationView {
                interpretation = Interpretation()
            }
        }
        createRootFor(vfc).requireInterpretation("")
    }

    @Test
    fun shouldShowEnteredText() = runTest {
        val enteredText = "And bring your flippers"
        val vfc = VFC {
            InterpretationView {
                scope = this@runTest
                api = Api(mock(config {}))
                interpretation = Interpretation()
                onInterpretationEdited = { (_) -> }
            }
        }
        with(createRootFor(vfc)) {
            enterInterpretation(enteredText)
            waitForEvents(timeout = 2 * DEBOUNCE_WAIT_PERIOD_MILLIS) //get past the debounce period
            requireInterpretation(enteredText)
        }
    }

    @Test
    fun shouldSaveVerifiedInterpretation() = runTest {
        val verifiedText = "And bring your flippers"
        val config = config {
            expectedInterpretation = Interpretation(verifiedText = verifiedText)
        }
        val vfc = VFC {
            InterpretationView {
                scope = this@runTest
                api = Api(mock(config))
                interpretation = Interpretation()
                onInterpretationEdited = { (_) -> }
            }
        }
        with(createRootFor(vfc)) {
            enterInterpretation(verifiedText)
            //assertion is in the mocked API call
        }
    }

    @Test
    fun shouldCallOnInterpretationEdited() = runTest {
        val verifiedText = "And bring your flippers"
        val config = config {
            expectedInterpretation = Interpretation(verifiedText = verifiedText)
        }
        var called = false
        val vfc = VFC {
            InterpretationView {
                scope = this@runTest
                api = Api(mock(config))
                interpretation = Interpretation()
                onInterpretationEdited = {
                    called = true
                }
            }
        }
        with(createRootFor(vfc)) {
            enterInterpretation(verifiedText)
            waitForEvents(timeout = 2 * DEBOUNCE_WAIT_PERIOD_MILLIS) //get past the debounce period
        }
        called shouldBe true
    }
}