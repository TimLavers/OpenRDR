package io.rippledown.kb

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.TestResult
import react.FC
import react.dom.test.runReactTest
import kotlin.test.Ignore
import kotlin.test.Test

class CreateKBTest {

    @Test
    fun dialogShouldNotBeShowingInitially(): TestResult {
        val vfc = FC {
            CreateKB {
            }
        }
        return runReactTest(vfc) { _ ->
            requireCreateKBDialogToNotBeShowing()
        }
    }

    @Test
    fun dialogShouldShowWhenMenuItemIsClicked(): TestResult {
        val vfc = FC {
            CreateKB {
            }
        }
        return runReactTest(vfc) { container ->
            with(container) {
                //given
                requireCreateKBDialogToNotBeShowing()

                //when
                showKBCreateDialog()

                //then
                requireCreateKBDialogToBeShowing()
            }
        }
    }

    @Test
    @Ignore
    fun onFinishShouldBeCalledWhenOKButtonIsClicked(): TestResult {
        var finishCalled = false
        val vfc = FC {
            CreateKB {
                onFinish = { finishCalled = true }
            }
        }
        return runReactTest(vfc) { container ->
            with(container) {
                //given
                showKBCreateDialog()
                enterNewProjectName("Bondi")

                //when
                //TODO this is not working
                clickConfirmCreateKBButton()

                //then
                finishCalled shouldBe true
                requireCreateKBDialogToNotBeShowing()
            }
        }
    }

    @Test
    @Ignore
    fun onFinishShouldBeCalledWhenCancelButtonIsClicked(): TestResult {
        var finished = false
        val vfc = FC {
            CreateKB {
                onFinish = { finished = true }
            }
        }
        return runReactTest(vfc) { container ->
            with(container) {
                //given
                createKBMenuItem().click()
                enterNewProjectName("Bondi")

                //when
                //TODO this is not working
                clickCancelCreateKBButton()

                //then
                finished shouldBe true
                requireCreateKBDialogToNotBeShowing()
            }
        }
    }

}