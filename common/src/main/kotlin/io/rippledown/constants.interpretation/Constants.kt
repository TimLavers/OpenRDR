package io.rippledown.constants.interpretation


const val INTERPRETATION_TAB_ORIGINAL_LABEL = "Report"
const val INTERPRETATION_TAB_CONCLUSIONS_LABEL = "Comments"
const val INTERPRETATION_TAB_CHANGES_LABEL = "Differences"
const val INTERPRETATION_TAB_PREFIX = "interpretation_tab_"

const val INTERPRETATION_TABS = "interpretation_tabs"
const val INTERPRETATION_TAB_ORIGINAL = "$INTERPRETATION_TAB_PREFIX$INTERPRETATION_TAB_ORIGINAL_LABEL"
const val INTERPRETATION_TAB_CONCLUSIONS = "$INTERPRETATION_TAB_PREFIX$INTERPRETATION_TAB_CONCLUSIONS_LABEL"
const val INTERPRETATION_PANEL_CONCLUSIONS = "interpretation_panel_$INTERPRETATION_TAB_CONCLUSIONS_LABEL"
const val NO_CONCLUSIONS = "NO_CONCLUSIONS"

const val INTERPRETATION_TEXT_FIELD = "interpretation_text_area"
const val INTERPRETATION_VIEW_LABEL = "Report for this case"
const val DEBOUNCE_WAIT_PERIOD_MILLIS = 1_000L

const val CHANGE_INTERPRETATION_BUTTON = "CHANGE_INTERPRETATION_BUTTON"
const val CHANGE_INTERPRETATION_DROPDOWN = "CHANGE_INTERPRETATION_DROPDOWN"
const val CHANGE_INTERPRETATION = "Change interpretation"

const val ADD_COMMENT_MENU = "ADD_COMMENT_MENU"
const val REPLACE_COMMENT_MENU = "REPLACE_COMMENT_MENU"
const val REMOVE_COMMENT_MENU = "REMOVE_COMMENT_MENU"

const val ADD_COMMENT = "Add a comment"
const val ADD_COMMENT_LABEL = "Select or create a comment to be added"
const val REPLACED_COMMENT_LABEL = "Select the comment to be replaced"
const val REPLACEMENT_COMMENT_LABEL = "Select or create a comment to be the replacement"
const val REMOVE_COMMENT_LABEL = "Select the comment to be removed"
const val NO_COMMENTS = "There are no comments given for this case"

const val REPLACE_COMMENT = "Replace a comment"
const val REMOVE_COMMENT = "Remove a comment"
const val OK = "OK"

const val CANCEL = "Cancel"
const val ADD_COMMENT_PREFIX = "ADD_COMMENT_PREFIX_"

const val REPLACE_COMMENT_PREFIX = "REPLACE_COMMENT_PREFIX_"
const val REPLACEMENT_COMMENT_PREFIX = "REPLACEMENT_COMMENT_PREFIX_"
const val REMOVE_COMMENT_PREFIX = "REMOVE_COMMENT_PREFIX_"
const val COMMENT_SELECTOR_TEXT_FIELD = "COMMENT_SELECTOR_TEXT_FIELD"

const val COMMENT_SELECTOR_LABEL = "COMMENT_SELECTOR_LABEL"

const val ADD_COMMENT_TEXT_FIELD = ADD_COMMENT_PREFIX + COMMENT_SELECTOR_TEXT_FIELD
const val REPLACEMENT_COMMENT_TEXT_FIELD = REPLACEMENT_COMMENT_PREFIX + COMMENT_SELECTOR_TEXT_FIELD
const val REMOVE_COMMENT_TEXT_FIELD = REMOVE_COMMENT_PREFIX + COMMENT_SELECTOR_TEXT_FIELD

const val OK_BUTTON_FOR_ADD_COMMENT = ADD_COMMENT_PREFIX + OK
const val OK_BUTTON_FOR_REPLACE_COMMENT = REPLACE_COMMENT_PREFIX + OK
const val OK_BUTTON_FOR_REMOVE_COMMENT = REMOVE_COMMENT_PREFIX + OK

const val CANCEL_BUTTON_FOR_ADD_COMMENT = "$ADD_COMMENT_PREFIX$CANCEL"
const val CANCEL_BUTTON_FOR_REPLACE_COMMENT = "$REPLACE_COMMENT_PREFIX$CANCEL"
const val CANCEL_BUTTON_FOR_REMOVE_COMMENT = "$REMOVE_COMMENT_PREFIX$CANCEL"



