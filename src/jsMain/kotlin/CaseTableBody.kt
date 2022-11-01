import csstype.rgb
import emotion.react.css
import io.rippledown.model.caseview.ViewableCase
import react.FC
import react.Props
import react.dom.html.ReactHTML

external interface CaseTableBodyHandler: Props {
    var case: ViewableCase
}
val CaseTableBody = FC<CaseTableBodyHandler> {
    ReactHTML.tbody {
        it.case.attributes().forEach { a ->
            val results = it.case.rdrCase.resultsFor(a)!!
            ReactHTML.tr {
                css {
                    nthChild("even") {
                        backgroundColor = rgb(224, 224, 224)
                    }
                }
                AttributeCell {
                    attribute = a
                }
                results.forEachIndexed() { i, result ->
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