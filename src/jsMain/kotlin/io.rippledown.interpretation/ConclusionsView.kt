package io.rippledown.interpretation

import io.rippledown.model.Interpretation
import mui.icons.material.CheckBoxOutlined
import mui.icons.material.ExpandLess
import mui.icons.material.ExpandMore
import mui.lab.TreeItem
import mui.lab.TreeView
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
    var interpretation: Interpretation
}

val ConclusionsView = FC<ConclusionsViewHandler> { handler ->
    val interpretation = handler.interpretation

    TreeView {
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



