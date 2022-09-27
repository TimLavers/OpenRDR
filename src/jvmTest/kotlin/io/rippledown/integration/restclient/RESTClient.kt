package io.rippledown.integration.restclient

import ADD_CONDITION
import CASE
import COMMIT_SESSION
import CREATE_KB
import START_SESSION
import WAITING_CASES
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.serializer.KotlinxSerializer
import io.ktor.http.*
import kotlinx.coroutines.*

import io.rippledown.model.CasesInfo
import io.rippledown.model.Conclusion
import io.rippledown.model.OperationResult
import io.rippledown.model.RDRCase
import io.rippledown.model.condition.Condition
import io.rippledown.model.condition.IsNormal
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json


class RESTClient {
    val endpoint = "http://127.0.0.1:9090"

    //    val jsonClient = HttpClient(CIO) {
//        install(JsonFeature) {
//            serializer = KotlinxSerializer(Json { allowStructuredMapKeys = true }) }
//    }
    val jsonClient = HttpClient {
        install(JsonFeature) { serializer = KotlinxSerializer(Json { allowStructuredMapKeys = true }) }
    }
    private lateinit var casesInfo: CasesInfo
    private var currentCase: RDRCase? = null

    init {
        getCaseIds()
    }

    private fun getCaseIds() {
        runBlocking {
            casesInfo = jsonClient.get(endpoint + WAITING_CASES)
        }
    }

    fun getCaseWithName(name: String): RDRCase? {
        getCaseIds()
        val caseId = casesInfo.caseIds.first { it.name == name }
        runBlocking {
            currentCase = jsonClient.get(endpoint + CASE + "?id=${caseId.id}")
        }
        return currentCase
    }

    fun startSessionToAddConclusionForCurrentCase(conclusion: Conclusion): OperationResult {
        require(currentCase != null)
        var result = OperationResult("")
        runBlocking {
            result = jsonClient.post(endpoint + START_SESSION + "?id=${currentCase!!.name}") {
                contentType(ContentType.Application.Json)
                body = conclusion
            }
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
                body = data
            }
        }
        return result
    }

    fun commitCurrentSession(): OperationResult {
        var result = OperationResult("")
        runBlocking {
            result = jsonClient.post(endpoint + COMMIT_SESSION) {
            }
        }
        return result
    }

    fun resetKB(): OperationResult {
        var result = OperationResult("")
        runBlocking {
            result = jsonClient.post(endpoint + CREATE_KB) {
            }
        }
        return result
    }
}