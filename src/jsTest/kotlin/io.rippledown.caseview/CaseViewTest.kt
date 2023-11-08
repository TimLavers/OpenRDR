package io.rippledown.caseview

import io.rippledown.model.createCase
import kotlinx.coroutines.test.TestResult
import react.FC
import react.dom.test.runReactTest
import kotlin.test.Test

class CaseViewTest {

    @Test
    fun shouldShowCaseName(): TestResult {
        val caseName = "case a "
        val fc = FC {
            CaseView {
                case = createCase(id = 1L, name = caseName)
            }
        }
        return runReactTest(fc) { container ->
            with(container) {
                requireCaseToBeShowing(caseName)
            }
        }
    }
}

