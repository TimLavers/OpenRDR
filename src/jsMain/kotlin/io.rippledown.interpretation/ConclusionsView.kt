package io.rippledown.interpretation

import io.rippledown.model.interpretationview.ViewableInterpretation
import mui.icons.material.CheckBoxOutlined
import mui.icons.material.ExpandLess
import mui.icons.material.ExpandMore
import mui.lab.TreeItem
import mui.lab.TreeView
import mui.lab.TreeViewPropsBase
import mui.material.Stack
import mui.material.StackDirection.Companion.row
import mui.material.SvgIconSize.Companion.small
import mui.material.Typography
import mui.material.styles.TypographyVariant.Companion.subtitle1
import mui.material.styles.TypographyVariant.Companion.subtitle2
import mui.system.responsive
import react.FC
import react.Props
import react.create
import react.createElement

external interface ConclusionsViewHandler : Props {
    var interpretation: ViewableInterpretation
}

val ConclusionsView = FC<ConclusionsViewHandler> { handler ->
    val interpretation = handler.interpretation

    TreeView {

        //workaround for a known bug in mui wrappers
        @Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
        this as TreeViewPropsBase

        interpretation.conclusions().forEach { conclusion ->
            TreeItem {
                nodeId = conclusion.text
                label = commentTypography(conclusion.text)
                expandIcon = createElement(ExpandMore)
                collapseIcon = createElement(ExpandLess)
                interpretation.conditionsForConclusion(conclusion).forEach { condition ->
                    TreeItem {
                        nodeId = condition
                        label = conditionTypography(condition)
                    }
                }
            }
        }
    }
}

fun commentTypography(text: String) = FC<Props> {
    Typography {
        id = "COMMENT:$text"
        variant = subtitle1
        +text
    }
}.create()

fun conditionTypography(text: String) = FC<Props> {
    Stack {
        direction = responsive(row)
        CheckBoxOutlined {
            fontSize = small
        }
        Typography {
            id = "CONDITION:$text"
            variant = subtitle2
            +text
        }
    }
}.create()



