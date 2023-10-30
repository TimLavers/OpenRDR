package io.rippledown.caseview

import io.rippledown.model.createCase
import react.FC
import react.dom.test.runReactTest
import kotlin.test.Test

class CaseViewTest {

    @Test
    fun shouldShowCaseName() {
        val caseName = "case a "
        val fc = FC {
            CaseView {
                case = createCase(id = 1L, name = caseName)
            }
        }
        runReactTest(fc) { container ->
            with(container) {
                requireCaseToBeShowing(caseName)
            }
        }
    }
}

