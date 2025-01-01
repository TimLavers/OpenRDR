package io.rippledown.conditiongenerator

import io.rippledown.expressionparser.AttributeFor
import io.rippledown.model.condition.CaseStructureCondition
import io.rippledown.model.condition.Condition
import io.rippledown.model.condition.EpisodicCondition
import io.rippledown.model.condition.SeriesCondition
import io.rippledown.model.condition.episodic.predicate.Contains
import io.rippledown.model.condition.episodic.predicate.TestResultPredicate
import io.rippledown.model.condition.episodic.signature.All
import io.rippledown.model.condition.episodic.signature.Signature
import io.rippledown.model.condition.series.Increasing
import io.rippledown.model.condition.series.SeriesPredicate
import io.rippledown.model.condition.structural.CaseStructurePredicate
import io.rippledown.model.condition.structural.IsSingleEpisodeCase
import kotlin.reflect.full.primaryConstructor

val EPISODIC_PREDICATE_PACKAGE = Contains::class.java.packageName
val SERIES_PREDICATE_PACKAGE = Increasing::class.java.packageName
val CASE_STRUCTURE_PREDICATE_PACKAGE = IsSingleEpisodeCase::class.java.packageName

//All the signatures are in the same package
val SIGNATURE_PACKAGE = All::class.java.packageName

class ConditionGenerator(private val attributeFor: AttributeFor) {

    fun conditionFor(attributeName: String, userExpression: String, conditionSpec: ConditionSpecification): Condition {
        val predicate = predicateFrom(conditionSpec.predicate)
        val attribute = attributeFor(attributeName)
        return when (predicate) {
            is TestResultPredicate -> {
                val signature = signatureFrom(conditionSpec.signature)
                EpisodicCondition(null, attribute, predicate, signature, userExpression)
            }

            is CaseStructurePredicate -> {
                CaseStructureCondition(null, predicate, userExpression)
            }

            is SeriesPredicate -> {
                SeriesCondition(null, attribute, predicate, userExpression)
            }

            else -> throw IllegalArgumentException("Unknown predicate type")
        }
    }

    fun predicateFrom(
        specification: FunctionSpecification,
    ): Any {
        val parameters = specification.parameters
        return try {
            predicateFromPackage(EPISODIC_PREDICATE_PACKAGE, specification, parameters)
        } catch (e: ClassNotFoundException) {
            try {
                predicateFromPackage(SERIES_PREDICATE_PACKAGE, specification, parameters)
            } catch (e: ClassNotFoundException) {
                predicateFromPackage(CASE_STRUCTURE_PREDICATE_PACKAGE, specification, parameters)
            }
        }
    }

    private fun predicateFromPackage(
        packageName: String,
        specification: FunctionSpecification,
        parameters: List<String>
    ): Any {
        val functionName = "$packageName.${specification.name}"
        return createInstance(functionName, *parameters.toTypedArray())
    }

    fun signatureFrom(specification: FunctionSpecification): Signature {
        val functionName = "$SIGNATURE_PACKAGE.${specification.name}"
        val parameters = specification.parameters
        return createInstance(functionName, *parameters.toTypedArray()) as Signature
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
}