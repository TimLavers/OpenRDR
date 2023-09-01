package io.rippledown.casecontrol

import Api
import io.kotest.assertions.asClue
import io.kotest.matchers.shouldBe
import io.rippledown.interpretation.*
import io.rippledown.model.Interpretation
import io.rippledown.model.createCase
import io.rippledown.model.diff.*
import io.rippledown.model.rule.SessionStartRequest
import kotlinx.coroutines.test.runTest
import mocks.config
import mocks.mock
import proxy.waitForEvents
import react.FC
import react.dom.createRootFor
import kotlin.test.Test

class CaseInspectionWhenStartingRuleBuildingTest {

    @Test
    fun shouldCallOnStartRuleWithExpectedInterpretation() = runTest {
        val bondiComment = "Go to Bondi now!"
        val manlyComment = "Go to Manly now!"
        val beachComment = "Enjoy the beach!"
        val diffList = DiffList(
            listOf(
                Unchanged(beachComment),
                Removal(manlyComment),
                Addition(bondiComment),
                Replacement(manlyComment, bondiComment)
            )
        )
        val caseA = createCase(
            id = 1L,
            name = "Manly",
        )

        val interp = Interpretation(
            diffList = diffList,
        )
        caseA.interpretation = interp

        val config = config {
            returnInterpretationAfterSavingInterpretation = interp
            expectedSessionStartRequest = SessionStartRequest(caseA.id!!, Addition(bondiComment))
        }

        var inProgress = false
        val fc = FC {
            CaseInspection {
                case = caseA
                scope = this@runTest
                api = Api(mock(config))
                ruleSessionInProgress = { it ->
                    inProgress = it
                }
            }
        }
        with(createRootFor(fc)) {
            //Given
            { "the first changed diff should be selected by default" }.asClue {
                requireBadgeCount(3)
            }

            //start to build a rule for the Addition
            selectChangesTab()
            waitForEvents()
            requireNumberOfRows(4)
            moveMouseOverRow(2)
            waitForEvents()

            //When
            clickBuildIconForRow(2)

            //Then
            inProgress shouldBe true

            //expected SessionStartRequest is checked in the config
        }
    }
}

