import csstype.*
import io.rippledown.model.RDRCase
import react.FC
import react.Props
import react.css.css
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.table
import react.dom.html.ReactHTML.td
import react.dom.html.ReactHTML.th
import react.dom.html.ReactHTML.tr

external interface CaseViewHandler : Props {
    var case: RDRCase
}

val CaseView = FC<CaseViewHandler> { props ->
    div {
        css {
            float = Float.left
            width = Length("70%")
            padding = px12
        }
        id = "case_view_container"
        div {
            +props.case.name
            id = "case_view_case_name"
            css {
                paddingBottom = px4
                paddingLeft = px8
                color = rdBlue
                fontStyle = FontStyle.italic
                fontWeight = FontWeight.bold
            }
        }
        table {
            css {
                border = Length("1px")
                borderColor = rgb( 128, 128, 128)
                borderStyle = LineStyle.solid
            }
            tr {
                th {
                    +"Attribute"
                    css {
                        padding = px8
                    }
                    id = "case_table_header_attribute"
                }
                th {
                    +"Value"
                    css {
                        padding = px8
                    }
                    id = "case_table_header_value"
                }
            }
            props.case.caseData.forEach {
                tr {
                    css {
                        nthChild("even") {
                            backgroundColor = rgb(224, 224, 224)
                        }
                    }
                    td {
                        +it.key
                        id = "attribute_name_cell_${it.key}"
                        css {
                            padding = px8
                        }
                    }
                    td {
                        +it.value
                        id = "attribute_value_cell_${it.key}"
                        css {
                            padding = px8
                        }
                    }
                }
            }
        }
    }
}
