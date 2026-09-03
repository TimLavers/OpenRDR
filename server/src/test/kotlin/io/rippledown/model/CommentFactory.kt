package io.rippledown.model

import io.rippledown.model.rule.AssignValue
import io.rippledown.model.rule.CommentTemplate

/**
 * Mints a comment attribute per comment text, as a knowledge base does, so
 * that tests can build rules that give comments.
 */
class CommentFactory(private val firstId: Int = 5000) {
    private val textToAttribute = mutableMapOf<String, Attribute>()

    fun attributeFor(text: String): Attribute = textToAttribute.getOrPut(text) {
        Attribute(firstId + textToAttribute.size, "C${textToAttribute.size + 1}", AttributeKind.COMMENT)
    }

    /**
     * The assignment that gives the comment with the given text.
     */
    fun comment(text: String) = AssignValue(attributeFor(text), CommentTemplate(text))
}
