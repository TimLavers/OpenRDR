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
        it.case.data.forEach {
            ReactHTML.tr {
                css {
                    nthChild("even") {
                        backgroundColor = rgb(224, 224, 224)
                    }
                }
                AttributeCell {
                    attribute = it.key.attribute
                }
                ValueCell {
                    attribute = it.key.attribute
                    value = it.value
                }
                ReferenceRangeCell {
                    attribute = it.key.attribute
                    value = it.value
                }
            }
        }
    }
}