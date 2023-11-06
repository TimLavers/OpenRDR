package io.rippledown.casecontrol

import io.kotest.assertions.asClue
import io.kotest.matchers.shouldBe
import io.rippledown.interpretation.*
import io.rippledown.model.createCase
import io.rippledown.model.diff.*
import io.rippledown.model.interpretationview.ViewableInterpretation
import io.rippledown.model.rule.SessionStartRequest
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.test.TestResult
import main.Api
import mocks.config
import mocks.mock
import proxy.waitForEvents
import react.FC
import react.dom.test.runReactTest
import kotlin.test.Test

class CaseInspectionWhenStartingRuleBuildingTest {

    @Test
    fun shouldCallOnStartRuleWithExpectedInterpretation(): TestResult {
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
        val interp = ViewableInterpretation().apply { this.diffList = diffList }
        val caseA = createCase(
            id = 1L,
            name = "Manly",
        ).apply { viewableInterpretation = interp }

        val config = config {
            returnInterpretationAfterSavingInterpretation = interp
            expectedSessionStartRequest = SessionStartRequest(caseA.id!!, Addition(bondiComment))
        }

        var inProgress = false
        val fc = FC {
            CaseInspection {
                case = caseA
                scope = MainScope()
                api = Api(mock(config))
                ruleSessionInProgress = { it ->
                    inProgress = it
                }
            }
        }
        return runReactTest(fc) { container ->
            with(container) {
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
}
