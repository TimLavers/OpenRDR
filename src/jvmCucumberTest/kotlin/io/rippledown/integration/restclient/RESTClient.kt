package io.rippledown.integration.restclient

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.rippledown.constants.api.*
import io.rippledown.constants.server.PING
import io.rippledown.constants.server.SHUTDOWN
import io.rippledown.model.*
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.condition.Condition
import io.rippledown.model.condition.tabular.TabularCondition
import io.rippledown.model.condition.tabular.chain.Current
import io.rippledown.model.condition.tabular.predicate.IsNotBlank
import io.rippledown.model.external.ExternalCase
import io.rippledown.server.routes.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json

class RESTClient {
    private val endpoint = "http://localhost:9090"

    private val jsonClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                allowStructuredMapKeys = true
            })
        }
    }

    private var currentCase: ViewableCase? = null

    fun serverHasStarted(): Boolean {
        return runBlocking {
            try {
                jsonClient.get("$endpoint$PING")
                true
            } catch (e: Exception) {
                false
            }
        }
    }

    fun getCaseWithName(name: String): ViewableCase? {
        runBlocking {
            val casesInfo: CasesInfo = jsonClient.get(endpoint + WAITING_CASES).body()
            val caseId = casesInfo.caseIds.first { it.name == name }
            currentCase = jsonClient.get(endpoint + CASE + "?id=${caseId.id}").body()
        }
        return currentCase
    }

    fun deleteProcessedCaseWithName(name: String) {
        runBlocking {
            jsonClient.delete(endpoint + DELETE_PROCESSED_CASE_WITH_NAME) {
                contentType(ContentType.Application.Json)
                setBody(CaseName(name))
            }
        }
    }

    fun getOrCreateAttribute(name: String): Attribute = runBlocking {
        jsonClient.post(endpoint + GET_OR_CREATE_ATTRIBUTE) {
            setBody(name)
        }.body()
    }

    fun getOrCreateConclusion(text: String): Conclusion = runBlocking {
        jsonClient.post(endpoint + GET_OR_CREATE_CONCLUSION) {
            setBody(text)
        }.body()
    }

    fun getOrCreateCondition(prototype: Condition): Condition = runBlocking {
        jsonClient.post(endpoint + GET_OR_CREATE_CONDITION) {
            contentType(ContentType.Application.Json)
            setBody(prototype)
        }.body()
    }

    fun provideCase(externalCase: ExternalCase): RDRCase = runBlocking {
        jsonClient.put(endpoint + PROCESS_CASE) {
            contentType(ContentType.Application.Json)
            setBody(externalCase)
        }.body()

    }

    fun startSessionToAddConclusionForCurrentCase(conclusion: Conclusion): OperationResult {
        require(currentCase != null)
        var result = OperationResult("")
        runBlocking {
            result = jsonClient.post(endpoint + START_SESSION_TO_ADD_CONCLUSION + "?id=${currentCase!!.id}") {
                contentType(ContentType.Application.Json)
                setBody(conclusion)
            }.body()
        }
        return result
    }

    fun startSessionToReplaceConclusionForCurrentCase(toGo: Conclusion, replacement: Conclusion): OperationResult {
        require(currentCase != null)
        var result = OperationResult("")
        runBlocking {
            result = jsonClient.post(endpoint + START_SESSION_TO_REPLACE_CONCLUSION + "?id=${currentCase!!.id}") {
                contentType(ContentType.Application.Json)
                setBody(listOf(toGo, replacement))
            }.body()
        }
        return result
    }

    fun addConditionForCurrentSession(condition: Condition): OperationResult {
        var result = OperationResult("")
        val data = Json.encodeToJsonElement(Condition.serializer(), condition)
        runBlocking {
            result = jsonClient.post(endpoint + ADD_CONDITION) {
                contentType(ContentType.Application.Json)
                setBody(data)
            }.body()
        }
        return result
    }

    fun commitCurrentSession(): OperationResult {
        var result = OperationResult("")
        runBlocking {
            result = jsonClient.post(endpoint + COMMIT_SESSION) {
            }.body()
        }
        return result
    }

    fun createRuleToAddText(caseName: String, text: String, conditionText: String = ""): OperationResult {
        getCaseWithName(caseName)
        val conclusion = getOrCreateConclusion(text)
        startSessionToAddConclusionForCurrentCase(conclusion)

        if (conditionText.isNotBlank()) {
            addConditionForCurrentSession(
                getOrCreateCondition(parseToCondition(conditionText))
            )
        }
        return commitCurrentSession()
    }

    private fun parseToCondition(conditionText: String): Condition {
        val firstWord = conditionText.split(" ")[0]
        val attribute = getOrCreateAttribute(firstWord)
        val remainderOfExpression = conditionText.substring(firstWord.length + 1)
        if (remainderOfExpression != "id not blank") {
            throw IllegalArgumentException("Only 'is not blank' is supported")
        }
        return TabularCondition(null, attribute, IsNotBlank, Current)
    }

    fun resetKB(): OperationResult {
        var result = OperationResult("")
        runBlocking {
            result = jsonClient.post(endpoint + CREATE_KB) {
            }.body()
        }
        return result
    }

    fun shutdown(): Unit = runBlocking {
        try {
            jsonClient.post("$endpoint$SHUTDOWN")
        } catch (e: Exception) {
            //expected
        }
    }
}