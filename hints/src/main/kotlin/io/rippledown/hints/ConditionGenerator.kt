package io.rippledown.hints

import io.rippledown.model.Attribute
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
        val attribute = if (attributeName.isNotBlank()) attributeFor(attributeName) else null
        val predicate = predicateFrom(conditionSpec.predicate, attribute)
        return when (predicate) {
            is TestResultPredicate -> {
                val signature = signatureFrom(conditionSpec.signature)
                println("signature = ${signature}, predicate = $predicate, userExpression = $userExpression, attribute = $attribute")
                EpisodicCondition(null, attribute!!, predicate, signature, userExpression)
            }

            is CaseStructurePredicate -> {
                CaseStructureCondition(null, predicate, userExpression)
            }

            is SeriesPredicate -> {
                SeriesCondition(null, attribute!!, predicate, userExpression)
            }

            else -> throw IllegalArgumentException("Unknown predicate type")
        }
    }

    fun predicateFrom(
        specification: FunctionSpecification,
        attribute: Attribute? = null,
    ): Any {
        val parameters = specification.parameters
        return try {
            episodicPredicate(specification, parameters)
        } catch (_: ClassNotFoundException) {
            try {
                seriesPredicate(specification, parameters)
            } catch (_: ClassNotFoundException) {
                caseStructurePredicate(specification, attribute)
            }
        }
    }

    private fun episodicPredicate(
        specification: FunctionSpecification,
        parameters: List<String>
    ): Any {
        val functionName = "$EPISODIC_PREDICATE_PACKAGE.${specification.name}"
        return createInstance(functionName, *parameters.toTypedArray())
    }

    private fun seriesPredicate(
        specification: FunctionSpecification,
        parameters: List<String>
    ): Any {
        val functionName = "$SERIES_PREDICATE_PACKAGE.${specification.name}"
        return createInstance(functionName, *parameters.toTypedArray())
    }

    private fun caseStructurePredicate(
        specification: FunctionSpecification,
        attribute: Attribute?
    ): Any {
        val functionName = "$CASE_STRUCTURE_PREDICATE_PACKAGE.${specification.name}"
        return createCaseStructureInstance(functionName, attribute)
    }

    fun signatureFrom(specification: FunctionSpecification): Signature {
        val functionName = "$SIGNATURE_PACKAGE.${specification.name}"
        val parameters = specification.parameters
        return createInstance(functionName, *parameters.toTypedArray()) as Signature
    }

    inline fun <reified T : Any> createInstance(className: String, vararg args: String?): T {
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
                    constructor.call(arg) as T
                }

                "kotlin.Double" -> {
                    val toDouble = arg!!.toDouble()
                    constructor.call(toDouble) as T
                }

                "kotlin.Int" -> {
                    val toInt = arg!!.toInt()
                    constructor.call(toInt) as T
                }

                else -> {
                    throw IllegalArgumentException("Unknown type for parameter")
                }
            }

        }
    }

    inline fun <reified T : Any> createCaseStructureInstance(className: String, attribute: Attribute?): T {
        val clazz = Class.forName(className).kotlin
        val constructor = clazz.primaryConstructor
        return if (constructor == null) {
            clazz.objectInstance as T
        } else {
            constructor.call(attribute) as T
        }
    }
}