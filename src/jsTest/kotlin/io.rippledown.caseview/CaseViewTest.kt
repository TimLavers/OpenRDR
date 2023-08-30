package io.rippledown.caseview

import io.rippledown.model.createCase
import kotlinx.coroutines.test.runTest
import react.FC
import react.dom.checkContainer
import kotlin.test.Test

class CaseViewTest {

    @Test
    fun shouldShowCaseName() = runTest {
        val name = "case a "
        val fc = FC {
            CaseView {
                case = createCase(name)
            }
        }
        checkContainer(fc) { container ->
            with(container) {
                requireCaseToBeShowing(name)
            }
        }
    }
}

