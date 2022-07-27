import api.getWaitingCasesInfo
import csstype.*
import io.rippledown.model.RDRCase
import io.rippledown.model.ReferenceRange
import io.rippledown.model.TestResult
import kotlinx.coroutines.launch
import react.FC
import react.Props
import react.css.css
import react.dom.html.ReactHTML
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.table
import react.dom.html.ReactHTML.tbody
import react.dom.html.ReactHTML.td
import react.dom.html.ReactHTML.textarea
import react.dom.html.ReactHTML.th
import react.dom.html.ReactHTML.thead
import react.dom.html.ReactHTML.tr

external interface CaseViewHandler : Props {
    var case: RDRCase
}

/**
 * A tabular representation of an RDRCase.
 *
 *  ORD2
 */
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
                borderColor = rgb(128, 128, 128)
                borderStyle = LineStyle.solid
            }
            thead {
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
                    th {
                        +"Reference Range"
                        css {
                            padding = px8
                        }
                        id = "case_table_header_reference_range"
                    }
                }
            }
            tbody {
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
                            +resultText(it.value)
                            id = "attribute_value_cell_${it.key}"
                            css {
                                padding = px8
                            }
                        }
                        td {
                            +rangeText(it.value.referenceRange)
                            id = "reference_range_cell_${it.key}"
                            css {
                                padding = px8
                            }
                        }
                    }
                }
            }
        }
        div {
            textarea {
                id = "interpretation_text_area"
                rows = 10
                cols = 72
                onChange = {
                    console.log("TA.onChange")
                }
            }
           div {
                button {
                    +"Send interpretation"
                    css {
                        padding = px4
                    }
                    onClick = {
                        console.log("Interp send clicked")
                    }
                    id = "send_interpretation_button"
                }
            }
        }
//        InterpretationView {
//            onSubmit = { input ->
//                console.log("InterpretationVew.onsubmit: $input")
//            }
//        }
    }
}

fun resultText(result: TestResult): String {
    val unit = result.units ?: ""
    return "${result.value.text} $unit"
}

fun rangeText(referenceRange: ReferenceRange?): String {
    if (referenceRange == null) {
        return ""
    }
    if (referenceRange.upperString == null) {
        return "(<${referenceRange.lowerString})"
    }
    if (referenceRange.lowerString == null) {
        return "(>${referenceRange.upperString})"
    }
    return "(${referenceRange.lowerString} - ${referenceRange.upperString})"
}