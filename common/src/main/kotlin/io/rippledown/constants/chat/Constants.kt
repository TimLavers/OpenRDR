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
const val WHICH_ATTRIBUTE = "which attribute"

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
 * attributes can be changed. The fragments are separate
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
const val COPY_CASE_TO_FAVOURITES = "CopyCaseToFavourites"
const val DELETE_CASE_FROM_FAVOURITES = "DeleteCaseFromFavourites"
const val COPY_CASE_TO_FAVOURITES_WITH_NEW_NAME = "CopyCaseToFavouritesWithNewName"

// Knowledge base management by chat. See documentation/design/kb_management_by_chat.md.
const val KB_INFO_PREFIX = "KbInfo:"
const val KB_CLOSED = "KbClosed"

const val LIST_KNOWLEDGE_BASES = "ListKnowledgeBases"
const val OPEN_KNOWLEDGE_BASE = "OpenKnowledgeBase"
const val CREATE_KNOWLEDGE_BASE = "CreateKnowledgeBase"
const val CLOSE_KNOWLEDGE_BASE = "CloseKnowledgeBase"
const val DELETE_KNOWLEDGE_BASE = "DeleteKnowledgeBase"
const val ADD_DEMONSTRATION_CASE = "AddDemonstrationCase"
const val RENAME_KNOWLEDGE_BASE = "RenameKnowledgeBase"
const val SHOW_KNOWLEDGE_BASE_DESCRIPTION = "ShowKnowledgeBaseDescription"
const val SET_KNOWLEDGE_BASE_DESCRIPTION = "SetKnowledgeBaseDescription"

const val NO_KB_OPEN_MESSAGE = "No knowledge base is open. Ask me to list, open or create one."
const val KB_ACTION_DURING_RULE_MESSAGE =
    "Please finish or cancel the current rule before opening, creating, closing or deleting a knowledge base."
const val NO_KNOWLEDGE_BASES = "There are no knowledge bases."
const val OPEN_SUFFIX = " (open)"
const val KB_OPENED = "Opened"
const val KB_CREATED = "Created and opened"
const val KB_CLOSED_MESSAGE = "Closed"
const val KB_DELETED = "Deleted"
const val CANNOT_BE_UNDONE = "This cannot be undone."
const val SAY_YES_TO_CONFIRM = "Say yes to confirm."
const val DID_YOU_MEAN = "Did you mean"
const val NO_KB_NAMED = "There is no knowledge base named"
const val THE_KNOWLEDGE_BASES_ARE = "The knowledge bases are:"
const val MORE_THAN_ONE_KB_MATCHES = "More than one knowledge base matches"
const val KB_ALREADY_EXISTS = "already exists"
const val HAS_NO_CASES = "has no cases"
const val EXTERNAL_INFORMATION_SYSTEM = "external information system"
const val PATHOLOGY_CASE = "pathology case"
const val MINIMAL_CASE = "minimal case"
const val DEMO_CASE_ADDED = "Added the case"
const val DEMO_CASE_NAME_MINIMAL = "Demo"
const val DEMO_CASE_NAME_PATHOLOGY = "Einstein"
const val NO_KB_OPEN = "No knowledge base is open."
const val NO_KBS_YET = "There are no knowledge bases yet."
const val KB_NAME_CANNOT_BE_BLANK = "A knowledge base name cannot be blank."

fun kbOpenedMessage(name: String) = "$KB_OPENED \"$name\"."
fun kbCreatedMessage(name: String) = "$KB_CREATED \"$name\"."
fun kbClosedMessage(name: String) = "$KB_CLOSED_MESSAGE \"$name\"."
fun kbDeletedMessage(name: String) = "$KB_DELETED \"$name\"."
fun kbRenamedMessage(oldName: String, newName: String) = "Renamed \"$oldName\" to \"$newName\"."
fun kbAlreadyExistsMessage(name: String) = "A knowledge base named \"$name\" $KB_ALREADY_EXISTS."
fun kbHasNoDescriptionMessage(name: String) = "\"$name\" has no description."
fun kbDescriptionUpdatedMessage(name: String) = "Description of \"$name\" updated."
fun confirmKbDeletionMessage(name: String) =
    "Delete the knowledge base \"$name\"? $CANNOT_BE_UNDONE $SAY_YES_TO_CONFIRM"

fun confirmKbOpenMessage(name: String) = "$DID_YOU_MEAN \"$name\"? Say yes to open it."
fun confirmKbCreateMessage(newName: String, existingName: String) =
    "There is already a knowledge base \"$existingName\". Create \"$newName\" as well? Say yes to create it."

fun kbNotFoundMessage(name: String, available: List<String>) =
    if (available.isEmpty()) "$NO_KB_NAMED \"$name\". $NO_KNOWLEDGE_BASES"
    else "$NO_KB_NAMED \"$name\". $THE_KNOWLEDGE_BASES_ARE ${available.joinToString(", ")}."

fun kbAmbiguousMessage(name: String, candidates: List<String>) =
    "$MORE_THAN_ONE_KB_MATCHES \"$name\": ${candidates.joinToString(", ")}. Which one?"

fun demoCaseAddedMessage(caseName: String) = "$DEMO_CASE_ADDED \"$caseName\"."
fun noKbGreeting(available: List<String>) =
    if (available.isEmpty()) "$NO_KBS_YET Say \"create D\" to create one."
    else "$NO_KB_OPEN $THE_KNOWLEDGE_BASES_ARE ${available.joinToString(", ")}. " +
            "Say \"open ${available.first()}\" to open one, or \"create D\" to create a new one."

fun emptyKbGreeting(kbName: String) =
    "The knowledge base \"$kbName\" $HAS_NO_CASES. Cases are normally provided by an $EXTERNAL_INFORMATION_SYSTEM. " +
            "To try it out, I can add a demonstration case: say \"$PATHOLOGY_CASE\" for a pathology report, " +
            "or \"$MINIMAL_CASE\" for a case with a single attribute."
