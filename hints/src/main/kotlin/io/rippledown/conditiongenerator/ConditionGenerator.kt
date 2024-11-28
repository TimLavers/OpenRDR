package io.rippledown.conditiongenerator

import io.rippledown.expressionparser.AttributeFor
import io.rippledown.model.condition.*
import kotlin.reflect.full.declaredFunctions

class ConditionGenerator(private val attributeFor: AttributeFor) {
    private val constructors = ConditionConstructors()

    /**
     * Generate a condition from an attribute name, user expression, and an array of tokens.
     *
     * For example, for attribute name "Glucose", user expression "glucose = 42" and tokens ["Equals", "42"], the condition will be generated
     * by calling the function Equals(Glucose, "glucose = 42", "42") where Glucose is the attribute corresponding to "Glucose"
     *
     * @param attributeName The name of the attribute to be used in the condition.
     * @param userExpression The user expression that generated the tokens
     * @param tokens The first token is the function name, the rest are parameters to that function
     */
    fun conditionFor(attributeName: String, userExpression: String, vararg tokens: String): Condition? {
        if (tokens.isEmpty()) return null
        val functionName = tokens[0]
        val attribute = attributeFor(attributeName)

        //Create the function's parameters by replacing the function name in the array of tokens with the attribute,
        //followed by the user expression, followed by the rest of the tokens
        val parameters = arrayOf(attribute, userExpression, *tokens.drop(1).toTypedArray())
        return callFunctionByName(functionName, *parameters)
    }

    private fun callFunctionByName(functionName: String, vararg args: Any?) = try {
        kFunction(functionName).call(constructors, *args) as? Condition
    } catch (e: Exception) {
        val params = args.joinToString(", ") { it.toString() }
        println("Error calling '$functionName' which requires ${kFunction(functionName).parameters.size} parameters")
        println("Parameters used: $params")
        e.printStackTrace()
        null
    }


    private fun kFunction(name: String) = constructors::class.declaredFunctions.find { it.name == name }
        ?: throw IllegalArgumentException("Function '$name' not found in ConditionConstructors")
}
