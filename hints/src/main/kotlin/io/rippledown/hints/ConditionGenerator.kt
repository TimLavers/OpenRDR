package io.rippledown.hints

import io.rippledown.log.lazyLogger
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
import kotlinx.coroutines.runBlocking
import kotlin.reflect.full.primaryConstructor

typealias AttributeFor = (String) -> Attribute

val EPISODIC_PREDICATE_PACKAGE: String = Contains::class.java.packageName
val SERIES_PREDICATE_PACKAGE: String = Increasing::class.java.packageName
val CASE_STRUCTURE_PREDICATE_PACKAGE: String = IsSingleEpisodeCase::class.java.packageName

//All the signatures are in the same package
val SIGNATURE_PACKAGE: String = All::class.java.packageName

class ConditionGenerator(
    private val attributeFor: AttributeFor,
    private val conditionChatService: ConditionChatService,
    private val attributeNames: List<String> = emptyList()
) {
    private val logger = lazyLogger

    fun conditionFor(userText: String): Condition? {
        if (userText.isBlank()) return null
        return try {
            val spec = runBlocking {
                conditionChatService.transform(userText)
            }
            spec?.let { conditionFor(it) }
        } catch (e: Exception) {
            logger.error("Failed to create condition for text: '$userText'", e)
            null
        }
    }

    fun conditionFor(conditionSpec: ConditionSpecification): Condition {
        val userExpression = conditionSpec.userExpression
        val attributeName = conditionSpec.attributeName
        val attribute = attributeName?.takeIf { it.isNotBlank() }?.let { attributeFor(it) }
        val predicate = predicateFrom(conditionSpec.predicate, attribute)
        return when (predicate) {
            is TestResultPredicate -> {
                requireNotNull(attribute) { "Attribute required for episodic condition" }
                val signature = signatureFrom(conditionSpec.signature)
                EpisodicCondition(null, attribute, predicate, signature, userExpression)
            }

            is CaseStructurePredicate -> {
                CaseStructureCondition(null, predicate, userExpression)
            }

            is SeriesPredicate -> {
                requireNotNull(attribute) { "Attribute required for series condition" }
                SeriesCondition(null, attribute, predicate, userExpression)
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

    fun createInstance(className: String, vararg args: String?): Any {
        val clazz = Class.forName(className).kotlin
        val constructor = clazz.primaryConstructor
        return if (constructor == null) {
            clazz.objectInstance
                ?: throw IllegalArgumentException("$className is not an object and has no primary constructor")
        } else {
            val arg = requireNotNull(args[0])  //Assume there is only one argument
            val constructorParameter = constructor.parameters[0]
            val type = constructorParameter.type
            when (type.toString()) {
                "kotlin.String" -> {
                    constructor.call(arg)
                }

                "kotlin.Double" -> {
                    constructor.call(arg.toDouble())
                }

                "kotlin.Int" -> {
                    constructor.call(arg.toInt())
                }

                else -> {
                    throw IllegalArgumentException("Unknown type for parameter")
                }
            }

        }
    }

    fun createCaseStructureInstance(className: String, attribute: Attribute?): Any {
        val clazz = Class.forName(className).kotlin
        val constructor = clazz.primaryConstructor
        return constructor?.call(attribute)
            ?: (clazz.objectInstance
                ?: throw IllegalArgumentException("$className is not an object and has no primary constructor"))
    }
}