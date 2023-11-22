package io.rippledown.caseview

import io.rippledown.constants.caseview.CASE_TABLE_ROW_PREFIX
import io.rippledown.main.Handler
import io.rippledown.model.caseview.ViewableCase
import mui.material.TableBody
import mui.material.TableRow
import mui.system.sx
import react.FC
import web.cssom.rgb

external interface CaseTableBodyHandler: Handler {
    var case: ViewableCase
    var onCaseEdited: () -> Unit
}
val CaseTableBody = FC<CaseTableBodyHandler> { handler ->
    TableBody {
        handler.case.attributes().forEach { a ->
            val results = handler.case.case.resultsFor(a)!!
            TableRow {
                id = "$CASE_TABLE_ROW_PREFIX${a.name}"
                hover = true
                sx {
                    nthOfType("even") {
                        backgroundColor = rgb(224, 224, 224)
                    }
                }
                AttributeCell {
                    attribute = a
                    api = handler.api
                    scope = handler.scope
                    onCaseEdited = handler.onCaseEdited
                }
                results.forEachIndexed { i, result ->
                    ValueCell {
                        index = i
                        attribute = a
                        value = result
                    }
                }
                ReferenceRangeCell {
                    attribute = a
                    value = results[0]
                }
            }
        }
    }
}