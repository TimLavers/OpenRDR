import csstype.rgb
import io.rippledown.model.RDRCase
import react.FC
import react.Props
import react.css.css
import react.dom.html.ReactHTML

external interface CaseTableBodyHandler: Props {
    var case: RDRCase
}
val CaseTableBody = FC<CaseTableBodyHandler> {
    ReactHTML.tbody {
        it.case.attributes.forEach { a ->
            val results = it.case.resultsFor(a)!!
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