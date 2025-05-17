package io.rippledown.constants.chat

const val CONFIRMATION_START = "Please confirm"
const val QUESTION_IF_THERE_ARE_EXISTING_COMMENTS =
    "Would you like to change the report? If so, you can add a comment, modify a comment, or remove a comment."
const val QUESTION_IF_THERE_ARE_NO_EXISTING_COMMENTS = "Would you like to add a comment to this report?"

const val CHAT_BOT_NO_RESPONSE_MESSAGE = "Sorry, I don't understand. Please try again."
const val CHAT_BOT_PLACEHOLDER = "Please enter your request..."
const val ADD_COMMENT_PLACEHOLDER = "{{ADDED}}"
const val CHAT_BOT_ADD_COMMENT_USER_MESSAGE = "the comment \"$ADD_COMMENT_PLACEHOLDER\" has been added."

const val DEBUG_ACTION = "debug"
const val USER_ACTION = "user"
const val ADD_ACTION = "add"
const val REMOVE_ACTION = "remove"
const val REPLACE_ACTION = "replace"
