package io.rippledown.integration.restclient

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.rippledown.constants.api.CASE
import io.rippledown.constants.api.CREATE_KB
import io.rippledown.constants.api.WAITING_CASES
import io.rippledown.model.CasesInfo
import io.rippledown.model.Conclusion
import io.rippledown.model.OperationResult
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.condition.Condition
import io.rippledown.server.PING
import io.rippledown.server.SHUTDOWN
import io.rippledown.server.routes.ADD_CONDITION
import io.rippledown.server.routes.COMMIT_SESSION
import io.rippledown.server.routes.START_SESSION_TO_ADD_CONCLUSION
import io.rippledown.server.routes.START_SESSION_TO_REPLACE_CONCLUSION
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json

class RESTClient {
    private val endpoint = "http://localhost:9090"

    private val jsonClient =  HttpClient(CIO) {
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
                jsonClient.get(endpoint + PING)
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

    fun startSessionToAddConclusionForCurrentCase(conclusion: Conclusion): OperationResult {
        require(currentCase != null)
        var result = OperationResult("")
        runBlocking {
            result = jsonClient.post(endpoint + START_SESSION_TO_ADD_CONCLUSION + "?id=${currentCase!!.name}") {
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
            result = jsonClient.post(endpoint + START_SESSION_TO_REPLACE_CONCLUSION + "?id=${currentCase!!.name}") {
                contentType(ContentType.Application.Json)
                setBody(listOf(toGo, replacement))
            }.body()
        }
        return result
    }

    fun addConditionForCurrentSession(condition: Condition): OperationResult {
        var result = OperationResult("")
        val data = Json.encodeToJsonElement(Condition.serializer(), condition)
        println("data: $data")
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

    fun createRuleToAddText(caseName: String, text: String): OperationResult {
        getCaseWithName(caseName)
        startSessionToAddConclusionForCurrentCase(Conclusion(text))
        return commitCurrentSession()
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
            jsonClient.post(endpoint + SHUTDOWN)
        } catch (e: Exception) {
            //expected
        }
    }
}