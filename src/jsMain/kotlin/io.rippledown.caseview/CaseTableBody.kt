package io.rippledown.caseview

import Handler
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
val CaseTableBody = FC<CaseTableBodyHandler> {
    TableBody {
        it.case.attributes().forEach { a ->
            val results = it.case.rdrCase.resultsFor(a)!!
            TableRow {
                id = "case_table_row_${a.name}"
                sx {
                    nthChild("even") {
                        backgroundColor = rgb(224, 224, 224)
                    }
                }
                AttributeCell {
                    attribute = a
                    api = it.api
                    scope = it.scope
                    onCaseEdited = it.onCaseEdited
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