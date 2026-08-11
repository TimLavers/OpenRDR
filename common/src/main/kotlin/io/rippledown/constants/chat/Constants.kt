package io.rippledown.constants.chat

const val CONFIRM = "confirm"
const val SUGGESTION = "suggestion"
const val REASON = "reason"
const val MORE_REASONS = "more reasons"
const val FIRST_REASON = "first reason"
const val IS_NOT_TRUE = "is not true"
const val ADD = "add"
const val REMOVE = "remove"
const val REPLACE = "replace"
const val STOP = "stop"
const val NO_COMMENTS = "No comments"
const val EXISTING_COMMENTS = "Existing comments"
const val WOULD_YOU_LIKE = "Would you like"
const val ADD_A_COMMENT = "add a comment"
const val REMOVE_A_COMMENT = "remove a comment"
const val REPLACE_A_COMMENT = "replace a comment"
const val WHAT_COMMENT = "What comment"
const val CHAT_BOT_NO_RESPONSE_MESSAGE = "Sorry, I don't understand. Please try again."
const val CHAT_BOT_PLACEHOLDER = "Your response..."
const val CHAT_BOT_DONE_MESSAGE = "Done"
const val START_ACTION = "start"

// Terminal bot responses: the conversation cannot make further progress after
// either of these, so anything waiting on the bot must give up at once rather
// than time out. Shared here because modules that only see the chat panel (the
// UI and the cucumber suite) need to recognise them.
const val AI_UNAVAILABLE_MESSAGE = "The AI assistant is temporarily unavailable. Please try again later."
const val SYSTEM_ERROR_PREFIX = "System error. See server.log"

// A distinctive word that the once-per-session "you can insert case values into a comment" tip must contain.
const val COMMENT_VARIABLE_TIP_KEYWORD = "braces"

/**
 * The messages about the names of comment and derived attributes. Comments are
 * named so that they can be referred to, and the names of KB-assigned
 * attributes can be changed; see step 14 of
 * documentation/design/repeat_inferencing.md. The fragments are separate
 * constants so that the cucumber suite can recognise the messages without
 * knowing the name, which the model chooses.
 */
const val COMMENT_IS_NAMED = "This comment is named"
const val CAN_BE_RENAMED = "You can rename it at any time."
const val RENAMED = "Renamed"
const val CANNOT_BE_RENAMED = "cannot be renamed"

/**
 * The message telling the user the name of the comment attribute created for
 * a comment they have just asked for, and that they can change it.
 */
fun commentNamedMessage(name: String) = "$COMMENT_IS_NAMED \"$name\". $CAN_BE_RENAMED"

fun renamedMessage(oldName: String, newName: String) = "$RENAMED \"$oldName\" to \"$newName\"."

fun cannotRenameMessage(name: String) =
    "\"$name\" is not a comment or a derived attribute, so it $CANNOT_BE_RENAMED."

const val ADD_COMMENT = "AddComment"
const val REMOVE_COMMENT = "RemoveComment"
const val REPLACE_COMMENT = "ReplaceComment"
const val ASSIGN_DERIVED_VALUE = "AssignDerivedValue"
const val REMOVE_DERIVED_VALUE = "RemoveDerivedValue"
const val REPLACE_DERIVED_VALUE = "ReplaceDerivedValue"
const val EDIT_DERIVED_DEFINITION = "EditDerivedAttributeDefinition"
const val DEBUG_ACTION = "DEBUG_ACTION"
const val USER_ACTION = "UserAction"
const val COMMIT_RULE = "CommitRule"
const val EXEMPT_CORNERSTONE = "ExemptCornerstone"
const val NEXT_CORNERSTONE = "NextCornerstone"
const val PREVIOUS_CORNERSTONE = "PreviousCornerstone"
const val UNDO_LAST_RULE = "UndoLastRule"
const val SHOW_LAST_RULE_FOR_UNDO = "ShowLastRuleForUndo"
const val MOVE_ATTRIBUTE = "MoveAttribute"
const val RENAME_ATTRIBUTE = "RenameAttribute"
const val SHOW_CORNERSTONES = "ShowCornerstones"
const val RULE_SESSION_COMPLETED = "RuleSessionCompleted"
const val CASES_INFO_PREFIX = "CasesInfo:"
const val REMOVE_REASON = "RemoveReason"
const val CANCEL_RULE = "CancelRule"


