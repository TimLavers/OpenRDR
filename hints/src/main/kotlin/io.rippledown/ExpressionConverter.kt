package io.rippledown

class ExpressionConverter(private val attributeNames: Collection<String>) {

    fun insertPlaceholder(ce: Expression) = attributeNames.fold(ce) { acc, attribute ->
        if (acc.text.contains(attribute, ignoreCase = true)) {
            Expression(acc.text.replace(attribute, placeHolder, ignoreCase = true), attribute)
        } else {
            acc
        }
    }

    fun removePlaceholder(ce: Expression) = with(ce) {
        Expression(text.replace(placeHolder, attribute), "")
    }

    companion object {
        val placeHolder = "x"
    }
}

data class Expression(val text: String, val attribute: String = "")