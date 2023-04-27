package io.rippledown.interpretation

import Handler
import csstype.Color
import csstype.px
import green
import io.rippledown.constants.interpretation.*
import io.rippledown.model.diff.Diff
import io.rippledown.model.diff.Unchanged
import mui.material.*
import mui.material.styles.TypographyVariant
import mui.system.sx
import react.FC
import red


external interface DiffViewerHandler : Handler {
    var changes: List<Diff>
}

val DiffViewer = FC<DiffViewerHandler> { handler ->
    TableContainer {
        title = "Changes"
        component = Paper
        Table {
            sx {
                minWidth = 400.px

            }
            TableHead {
                TableRow {
                    sx {
                        paddingTop = 5.px
                        paddingBottom = 5.px
                        height = 30.px
                    }
                    TableCell {
                        Typography {
                            +"Select rule action"
                            variant = TypographyVariant.subtitle2
                        }
                    }

                    TableCell {
                        Typography {
                            +"Original"
                            variant = TypographyVariant.subtitle2
                        }
                    }
                    TableCell {
                        Typography {
                            +"Changed"
                            variant = TypographyVariant.subtitle2
                        }
                    }
                }
            }
            TableBody {
                id = DIFF_VIEWER_TABLE
                handler.changes.forEachIndexed() { index, change ->
                    TableRow {
                        id = "$DIFF_VIEWER_ROW$index"
                        sx {
                            padding = 5.px
                            height = 10.px
                        }
                        TableCell {
                            sx {
                                padding = 5.px
                                height = 10.px
                            }
                            if (change !is Unchanged) {
                                Checkbox {
                                    id = "$DIFF_VIEWER_CHECKBOX$index"
                                    sx {
                                        color = Color("primary.main")
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
                    }
                }
            }

        }
    }
}
