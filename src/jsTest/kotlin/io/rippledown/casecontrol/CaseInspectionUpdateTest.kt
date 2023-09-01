package io.rippledown.casecontrol

import Api
import io.rippledown.interpretation.*
import io.rippledown.model.Interpretation
import io.rippledown.model.createCase
import io.rippledown.model.diff.Addition
import io.rippledown.model.diff.DiffList
import io.rippledown.model.rule.SessionStartRequest
import kotlinx.coroutines.test.runTest
import mocks.config
import mocks.mock
import proxy.waitForEvents
import react.FC
import react.dom.createRootFor
import kotlin.test.Test

class CaseInspectionUpdateTest {

    @Test
    fun shouldUpdateDiffViewAfterARuleIsBuilt() = runTest {
        val bondiComment = "Go to Bondi now!"
        val diffList = DiffList(
            listOf(
                Addition(bondiComment)
            )
        )
        val caseA = createCase(
            id = 1L,
            name = "Manly",
        )

        val interpBeforeRule = Interpretation(
            verifiedText = bondiComment,
            diffList = diffList
        )

        val interpAfterRule = Interpretation(
            verifiedText = bondiComment
        )

        caseA.interpretation = interpBeforeRule

        val config = config {
            returnInterpretationAfterSavingInterpretation = interpBeforeRule
            returnInterpretationAfterBuildingRule = interpAfterRule
            expectedSessionStartRequest = SessionStartRequest(caseA.id!!, Addition(bondiComment))
        }

        val fc = FC {
            CaseInspection {
                case = caseA
                scope = this@runTest
                api = Api(mock(config))
                ruleSessionInProgress = { _ -> Unit }
            }
        }
        with(createRootFor(fc)) {
            //Given
            requireBadgeCount(1)
            selectChangesTab()
            requireNumberOfRows(1)
            clickBuildIconForRow(0)

            //When a rule is built
            clickDoneButton()
            waitForEvents()

            //Then
            requireNoBadge()
            waitForEvents()
            requireNoBadge()
        }
    }

}

