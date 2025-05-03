package io.rippledown.chat

import io.rippledown.model.RDRCase

fun responseFor(message: String, case: RDRCase): String {
    return when (message) {
        "add a comment" -> "Please provide the comment you would like to add."
        "delete a comment" -> "Please provide the comment you would like to delete."
        "update a comment" -> "Please provide the updated comment."
        else -> "I'm sorry, I didn't understand that. Can you please rephrase?"
    }
}