package io.rippledown.integration.restclient

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.rippledown.constants.api.*
import io.rippledown.constants.server.*
import io.rippledown.main.Api
import io.rippledown.model.*
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.condition.Condition
import io.rippledown.model.condition.EpisodicCondition
import io.rippledown.model.condition.RuleConditionList
import io.rippledown.model.condition.episodic.predicate.IsNotBlank
import io.rippledown.model.condition.episodic.signature.Current
import io.rippledown.model.diff.Addition
import io.rippledown.model.external.ExternalCase
import io.rippledown.model.rule.RuleRequest
import io.rippledown.model.rule.SessionStartRequest
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import java.util.concurrent.atomic.AtomicReference


class RESTClient {
    private val endpoint = "http://localhost:9090"
    private val api = Api()

    private val jsonClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                allowStructuredMapKeys = true
            })
        }
    }

    private var currentKB: AtomicReference<KBInfo> = AtomicReference<KBInfo>()
    private var currentCase: ViewableCase? = null

    fun serverIsRunning(): Boolean {
        return runBlocking {
            try {
                jsonClient.get("$endpoint$PING")
                true
            } catch (_: Exception) {
                false
            }
        }
    }

    fun getCaseWithName(name: String): ViewableCase? {
        runBlocking {
            val casesInfo: CasesInfo = jsonClient.get(endpoint + WAITING_CASES) {
                parameter(KB_ID, currentKB.get()!!.id)
            }.body()
            val caseId = casesInfo.caseIds.first { it.name == name }
            currentCase = jsonClient.get(endpoint + CASE) {
                parameter(CASE_ID, caseId.id)
                parameter(KB_ID, currentKB.get()!!.id)
            }.body()
        }
        return currentCase
    }

    fun deleteProcessedCaseWithName(name: String) {
        runBlocking {
            Api().deleteCase(name)
        }
    }

    fun getOrCreateAttribute(name: String): Attribute = runBlocking {
        jsonClient.post(endpoint + GET_OR_CREATE_ATTRIBUTE) {
            setBody(name)
            parameter(KB_ID, currentKB.get().id)
        }.body()
    }

    fun getOrCreateConclusion(text: String): Conclusion = runBlocking {
        jsonClient.post(endpoint + GET_OR_CREATE_CONCLUSION) {
            setBody(text)
            parameter(KB_ID, currentKB.get().id)
        }.body()
    }

    fun getOrCreateCondition(prototype: Condition): Condition = runBlocking {
        jsonClient.post(endpoint + GET_OR_CREATE_CONDITION) {
            contentType(ContentType.Application.Json)
            setBody(prototype)
            parameter(KB_ID, currentKB.get().id)
        }.body()
    }

    fun provideCase(externalCase: ExternalCase): RDRCase {
        val result = runBlocking {
            jsonClient.put(endpoint + PROCESS_CASE) {
                contentType(ContentType.Application.Json)
                setBody(externalCase)
                parameter(KB_ID, currentKB.get().id)
            }.body<RDRCase>()
        }
        return result
    }

    fun createRuleToAddText(caseName: String, text: String, vararg conditions: String = arrayOf()): ViewableCase {
        val currentCase = getCaseWithName(caseName)!!
        val sessionStartRequest = SessionStartRequest(currentCase.id!!, Addition(text))
        runBlocking { api.startRuleSession(sessionStartRequest) }

        val listOfConditions = conditions.map { conditionText ->
                getOrCreateCondition(parseToCondition(conditionText))
        }

        val ruleRequest = RuleRequest(currentCase.id!!, RuleConditionList(listOfConditions))
        return runBlocking { api.commitSession(ruleRequest) }
    }

    private fun parseToCondition(conditionText: String): Condition {
        val firstWord = conditionText.split(" ")[0]
        val attribute = getOrCreateAttribute(firstWord)
        val remainderOfExpression = conditionText.substring(firstWord.length + 1)
        if (remainderOfExpression != "is in case") {
            throw IllegalArgumentException("Only 'is in case' is supported")
        }
        return EpisodicCondition(null, attribute, IsNotBlank, Current)
    }

    fun createKB(name: String) {
        runBlocking {
            val kbi = api.createKB(name)
            currentKB.set(kbi)
        }
    }

    fun createKBWithDefaultName() = createKB(DEFAULT_PROJECT_NAME)

    fun shutdown(): Unit = runBlocking {
        try {
            jsonClient.post("$endpoint$SHUTDOWN")
        } catch (_: Exception) {
            //expected
        } finally {
            jsonClient.close()
        }
    }
}