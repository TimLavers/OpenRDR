import io.rippledown.interpretation.*
import io.rippledown.model.CaseId
import io.rippledown.model.Interpretation
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.condition.ConditionList
import io.rippledown.model.diff.RuleRequest
import kotlinx.coroutines.launch
import mui.material.Grid
import mui.material.List
import mui.material.ListItemButton
import mui.material.ListItemText
import mui.system.sx
import react.FC
import react.memo
import react.useState
import web.cssom.Cursor.Companion.pointer
import web.cssom.Overflow
import web.cssom.px

const val CASELIST_ID = "case_list_container"
const val CASE_ID_PREFIX = "case_list_item_"

external interface CaseListHandler : Handler {
    var caseIds: List<CaseId>
}

val CaseList = FC<CaseListHandler> { handler ->
    var currentCase: ViewableCase? by useState(null)
    var newInterpretation: Interpretation? by useState(null)
    var conditionHints: ConditionList? by useState(null)

    fun updateCurrentCase(id: String) {
        handler.scope.launch {
            val returned = handler.api.getCase(id)
            currentCase = returned
        }
    }

    fun selectFirstCase() {
        val names = handler.caseIds.map { it.name }
        val currentCaseNullOrNotAvailable = currentCase == null || !names.contains(currentCase?.name)
        if (currentCaseNullOrNotAvailable && names.isNotEmpty()) {
            val firstCaseId = handler.caseIds[0]
            updateCurrentCase(firstCaseId.name)
        }
    }
    selectFirstCase()

    Grid {
        container = true
        Grid {
            item = true
            id = CASELIST_ID
            xs = 2
            List {
                sx {
                    cursor = pointer
                    width = 200.px
                    overflowY = Overflow.scroll
                    maxHeight = 500.px
                }
                dense = true

                for (caseId in handler.caseIds) {
                    ListItemButton {
                        ListItemText {
                            +caseId.name
                        }
                        selected = currentCase?.name == caseId.name
                        id = "$CASE_ID_PREFIX${caseId.name}"
                        onClick = {
                            updateCurrentCase(caseId.name)
                        }
                        sx {
                            paddingTop = 0.px
                            paddingBottom = 0.px
                        }
                    }
                }
            }
        }
        Grid {
            item = true
            xs = 6
            if (currentCase != null) {
                CaseView {
                    scope = handler.scope
                    api = handler.api
                    case = currentCase!!
                    onCaseEdited = {
                        updateCurrentCase(currentCase!!.name)
                    }
                    onStartRule = { newInterp ->
                        newInterpretation = newInterp
                        handler.scope.launch {
                            conditionHints = handler.api.conditionHints(currentCase!!.name)
                        }
                    }
                }
            }
        }

        Grid {
            item = true
            xs = 4
            if (newInterpretation != null && conditionHints != null) {
                ConditionSelector {
                    scope = handler.scope
                    api = handler.api
                    conditions = conditionHints!!.conditions
                    onCancel = {
                        newInterpretation = null
                    }
                    onDone = { conditionList ->
                        handler.scope.launch {
                            val ruleRequest = RuleRequest(
                                caseId = currentCase!!.name,
                                diffList = newInterpretation!!.diffList,
                                conditionList = ConditionList(conditions = conditionList)
                            )
                            handler.api.buildRule(ruleRequest)
                            newInterpretation = null
                            updateCurrentCase(currentCase!!.name)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Memoize the CaseList component so that it is not re-rendered when the caseIds don't change.
 */
val CaseListMemo = memo(
    type = CaseList,
    propsAreEqual = { prevProps, nextProps ->
        prevProps.caseIds == nextProps.caseIds
    }
)

fun dummyConditions(): List<Condition> {
    val tsh = Attribute("TSH", 1)
    val ft4 = Attribute("FT4", 2)
    return listOf(
        IsNormal(0, tsh),
        HasCurrentValue(1, tsh),
        Is(2, tsh, "0.667"),
        HasNoCurrentValue(3, ft4)
    )
}

