package io.rippledown.conditiongenerator

import io.rippledown.expressionparser.AttributeFor
import io.rippledown.model.condition.Condition
import io.rippledown.model.condition.ConditionConstructors
import io.rippledown.model.condition.EpisodicCondition
import io.rippledown.model.condition.episodic.predicate.Contains
import io.rippledown.model.condition.episodic.predicate.TestResultPredicate
import io.rippledown.model.condition.episodic.signature.All
import io.rippledown.model.condition.episodic.signature.Signature
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.full.primaryConstructor

//All the predicates are in the same package
val PREDICATE_PACKAGE = Contains::class.java.packageName

//All the signatures are in the same package
val SIGNATURE_PACKAGE = All::class.java.packageName

class ConditionGenerator(private val attributeFor: AttributeFor) {
    private val constructors = ConditionConstructors()

    fun conditionFor(attributeName: String, userExpression: String, conditionSpec: ConditionSpecification): Condition {
        val predicate = predicateFrom(conditionSpec.predicate)
        val signature = signatureFrom(conditionSpec.signature)
        val attribute = attributeFor(attributeName)
        val condition = EpisodicCondition(null, attribute, predicate, signature, userExpression)
        return condition
    }

    fun predicateFrom(
        specification: FunctionSpecification,
    ): TestResultPredicate {
        val functionName = "$PREDICATE_PACKAGE.${specification.name}"
        val parameters = specification.parameters
        return createInstance(functionName, *parameters.toTypedArray()) as TestResultPredicate
    }

    fun signatureFrom(specification: FunctionSpecification): Signature {
        val functionName = "$SIGNATURE_PACKAGE.${specification.name}"
        val parameters = specification.parameters
        return createInstance(functionName, *parameters.toTypedArray()) as Signature
    }

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

        //Create the function's parameters by replacing the function name in the array of tokens with the attribute, if there was one,
        //followed by the user expression, followed by the rest of the tokens
        val parameters = if (attributeName.isNotBlank()) {
            val attribute = attributeFor(attributeName)
            arrayOf(attribute, userExpression, *tokens.drop(1).toTypedArray())
        } else {
            arrayOf(userExpression, *tokens.drop(1).toTypedArray())
        }
        return callFunctionByName(functionName, *parameters) as? Condition
    }

    private fun callFunctionByName(functionName: String, vararg args: Any?) = try {
        kFunction(functionName).call(constructors, *args)
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


fun <T : Any> createInstance(className: String, vararg args: String?): T {
    val clazz = Class.forName(className).kotlin
    val constructor = clazz.primaryConstructor
    return if (constructor == null) {
        clazz.objectInstance as T
    } else {
        val arg = args[0]  //Assume there is only one argument
        val constructorParameter = constructor.parameters[0]
        val type = constructorParameter.type
        when (type.toString()) {
            "kotlin.String" -> {
                println("class $className has a string parameter")
                constructor.call(arg) as T
            }

            "kotlin.Double" -> {
                println("class $className has a double parameter")
                val toDouble = arg!!.toDouble()
                println("toDouble = $toDouble")
                constructor.call(toDouble) as T
            }

            "kotlin.Int" -> {
                println("class $className has a double parameter")
                val toInt = arg!!.toInt()
                println("toInt = $toInt")
                constructor.call(toInt) as T
            }

            else -> {
                println("class $className has a parameter of unknown type $type")
                throw IllegalArgumentException("Unknown type for parameter")
            }
        }

    }
}