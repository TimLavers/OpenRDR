package io.rippledown.kb

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.TestResult
import react.FC
import react.dom.test.runReactTest
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
    fun newProjectNameShouldBeSetWhenOKButtonIsClicked(): TestResult {
        var projectName = ""
        val vfc = FC {
            CreateKB {
                onFinish = { name ->
                    projectName = name
                }
            }
        }
        return runReactTest(vfc) { container ->
            with(container) {
                //given
                showKBCreateDialog()
                enterNewProjectName("Bondi")

                //when
                buttons()
//                clickConfirmCreateKBButton()

                //then
                projectName shouldBe "Bondi"
                requireCreateKBDialogToNotBeShowing()
            }
        }
    }

    @Test
    fun newProjectNameShouldNotBeSetWhenCancelButtonIsClicked(): TestResult {
        var projectName = ""
        val vfc = FC {
            CreateKB {
                onFinish = { name ->
                    projectName = name
                }
            }
        }
        return runReactTest(vfc) { container ->
            with(container) {
                //given
                createKBMenuItem().click()
                enterNewProjectName("Bondi")

                //when
                clickCancelCreateKBButton()

                //then
                projectName shouldBe null
                requireCreateKBDialogToNotBeShowing()
            }
        }
    }

}