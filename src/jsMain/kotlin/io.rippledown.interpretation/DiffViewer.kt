package io.rippledown.interpretation

import Handler
import green
import io.rippledown.constants.interpretation.*
import io.rippledown.model.Interpretation
import io.rippledown.model.diff.Unchanged
import mui.icons.material.Build
import mui.material.*
import mui.material.styles.TypographyVariant.Companion.subtitle2
import mui.system.sx
import react.FC
import react.ReactNode
import react.useState
import red
import web.cssom.Cursor.Companion.pointer
import web.cssom.px


external interface DiffViewerHandler : Handler {
    var interpretation: Interpretation
    var onStartRule: (interp: Interpretation) -> Unit
}

val DiffViewer = FC<DiffViewerHandler> { handler ->
    val diffList = handler.interpretation.diffList
    var cursorOnRow by useState(diffList.indexOfFirstChange())

    TableContainer {
        component = Paper
        Table {
            sx {
                minWidth = 400.px
                cursor = pointer
            }
            TableHead {
                TableRow {
                    sx {
                        padding = 5.px
                        height = 30.px
                    }

                    TableCell {
                        Typography {
                            +"Original"
                            variant = subtitle2
                        }
                    }
                    TableCell {
                        Typography {
                            +"Current"
                            variant = subtitle2
                        }
                    }
                    TableCell {
                    }

                }
            }
            TableBody {
                id = DIFF_VIEWER_TABLE
                diffList.diffs.forEachIndexed() { index, change ->
                    TableRow {
                        id = "$DIFF_VIEWER_ROW$index"
                        sx {
                            padding = 5.px
                            height = 10.px
                        }
                        onMouseOver = {
                            cursorOnRow = index
                        }

                        TableCell {
                            sx {
                                padding = 5.px
                                height = 10.px
                            }
                            Typography {
                                id = "$DIFF_VIEWER_ORIGINAL$index"
                                +change.left()
                                if (change !is Unchanged) {
                                    sx {
                                        backgroundColor = red
                                    }
                                }
                            }
                        }
                        TableCell {
                            sx {
                                padding = 5.px
                                height = 10.px
                            }
                            Typography {
                                id = "$DIFF_VIEWER_CHANGED$index"
                                +change.right()
                                if (change !is Unchanged) {
                                    sx {
                                        backgroundColor = green
                                    }
                                }
                            }
                        }
                        TableCell {
                            sx {
                                padding = 5.px
                                height = 10.px
                                minWidth = 50.px
                            }
                            if (change !is Unchanged && cursorOnRow == index) {
                                Tooltip {
                                    title = "Build a rule for this change".unsafeCast<ReactNode>()
                                    IconButton {
                                        sx {
                                            padding = 5.px
                                            height = 10.px
                                        }
                                        Build {
                                        }
                                        id = "$DIFF_VIEWER_BUILD_ICON$index"
                                        onClick = {
                                            diffList.selected = index //identify the change in the interpretation
                                            handler.onStartRule(handler.interpretation) //show the condition selector
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

        }
    }

}


