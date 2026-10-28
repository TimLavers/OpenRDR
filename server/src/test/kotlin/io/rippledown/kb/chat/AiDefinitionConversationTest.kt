package io.rippledown.kb.chat

import com.google.common.collect.ImmutableList
import com.google.genai.Chat
import com.google.genai.types.FunctionCall
import com.google.genai.types.GenerateContentResponse
import io.kotest.matchers.shouldBe
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.every
import io.mockk.slot
import io.rippledown.chat.ChatService
import io.rippledown.chat.Conversation
import io.rippledown.chat.Conversation.Companion.REASON_PARAMETER
import io.rippledown.chat.Conversation.Companion.TRANSFORM_REASON
import io.rippledown.chat.ConversationService
import io.rippledown.chat.ReasonTransformer
import io.rippledown.constants.chat.EXPLAIN_AI_EVALUATION
import io.rippledown.constants.chat.COMMIT_RULE
import io.rippledown.kb.chat.RuleConversation.Companion.MORE_REASONS_QUESTION
import io.rippledown.fromJsonString
import io.rippledown.kb.KB
import io.rippledown.kb.KBSession
import io.rippledown.kb.ai.*
import io.rippledown.kb.chat.action.ProposeAiDerivedAttribute
import io.rippledown.model.*
import io.rippledown.model.rule.AiBoolean
import io.rippledown.model.rule.evaluation.AiEvaluationRequest
import io.rippledown.persistence.inmemory.InMemoryKB
import io.rippledown.server.KBEndpoint
import io.rippledown.toJsonString
import io.rippledown.utils.defaultDate
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.util.Optional

class AiDefinitionConversationTest {
    private val kb = KB(InMemoryKB(KBInfo("chat_ai", "AI chat")))
    private val session = KBSession(kb)
    private val manager = session.ruleSessionManagerFor(UserId("expert"))
    private val notes = kb.attributeManager.getOrCreate("clinical_notes")
    private val case = kb.addProcessedCase(RDRCaseBuilder().apply {
        addValue(notes, defaultDate, "T2 Diab")
    }.build("Current"))
    private val view = kb.viewableCaseOf(case)
    private var calls = 0
    private var fail = false
    private val classifier = object : AiBooleanClassifier {
        override suspend fun classify(request: AiEvaluationRequest): AiClassification {
            calls++
            if (fail) return AiClassification.Failed("Provider unavailable")
            return AiClassification.Classified(true, "T2 Diab", "test-model")
        }
    }
    private val service = LockedRuleService(session, manager,
        AiInferenceCoordinator(session, AiEvaluationService(kb.aiEvaluationStore, classifier)))
    private val model = mockk<ConversationService>().also {
        coEvery { it.startConversation(any()) } returns "Hello"
        coEvery { it.response(any()) } returns "Ready for review."
    }
    private val chat = ChatManager(model, service, mockk())
    private val json = """{
        "action":"ProposeAiDerivedAttribute", "attributeName":"hasDiabetes",
        "sourceName":"clinical_notes", "instructions":"Personal history; exclude family history, suspected and gestational diabetes alone.",
        "examples":[{"input":"FH diab","expected":false},{"input":"T2 Diab","expected":true}]
    }"""

    @Test
    fun `structured AI actions preserve criteria and labelled examples`() {
        // Given structured authoring output
        // When the explicit constructor mapping creates the action
        val action = json.fromJsonString<ActionComment>().createActionInstance().shouldBeInstanceOf<ProposeAiDerivedAttribute>()

        // Then no formula parsing or Boolean coercion is involved
        action.sourceName shouldBe "clinical_notes"
        action.examples.map { it.expected } shouldBe listOf(false, true)
    }

    @Test
    fun `a bare acceptance starts the exact displayed draft without asking the model to reconstruct it`() = runTest {
        // Given a complete specification displayed with its preview
        chat.startConversation(view)
        val response = chat.processActionComment(json.fromJsonString())
        val draft = requireNotNull(service.currentAiDefinitionDraft())
        response.text shouldContain "Preview: true"
        response.text shouldContain "FH diab → false"
        val view = requireNotNull(response.draft)
        view.attributeName shouldBe "hasDiabetes"
        view.source shouldBe "clinical_notes"
        view.previewValue shouldBe "true"
        view.prompt shouldBe "Accept this definition?"

        // When the user accepts and completes the ordinary rule workflow
        val accepted = chat.response("yes")

        // Then the server asks for reasons while the assignment rule is still pending
        accepted.text shouldBe "Accepted the definition of \"hasDiabetes\".\n\n$MORE_REASONS_QUESTION"
        manager.isRuleSessionActive() shouldBe true
        kb.attributeManager.byName("hasDiabetes").shouldBeNull()
        coVerify(exactly = 0) { model.response(any()) }

        // When the rule is subsequently committed
        service.commitCurrentRuleSession()

        // Then the server's draft is committed unchanged
        val attribute = requireNotNull(kb.attributeManager.byName("hasDiabetes"))
        kb.derivedDefinitionManager.definitionFor(attribute.id) shouldBe draft.definition
        calls shouldBe 1
        coVerify(exactly = 0) { model.response("yes") }
    }

    @Test
    fun `declining reasons after AI acceptance answers the server question and commits the pending rule`() = runTest {
        // Given an accepted definition and the server's question about reasons
        chat.startConversation(view)
        chat.processActionComment(json.fromJsonString())
        chat.response("yes")
        val nextTurn = slot<String>()
        coEvery { model.response(capture(nextTurn)) } returns ActionComment(COMMIT_RULE).toJsonString()

        // When the expert declines to add reasons
        chat.response("no")

        // Then the model sees the actual pending question and the active rule's status
        nextTurn.captured shouldContain "[The server asked the user: $MORE_REASONS_QUESTION]"
        nextTurn.captured shouldContain "Current cornerstone status:"
        manager.isRuleSessionActive() shouldBe false
        val attribute = requireNotNull(kb.attributeManager.byName("hasDiabetes"))
        kb.materialise(case).latestValue(attribute) shouldBe "true"
        calls shouldBe 1
    }

    @Test
    fun `cancelling a displayed draft leaves the name reusable`() = runTest {
        // Given a displayed draft
        chat.startConversation(view)
        chat.processActionComment(json.fromJsonString())

        // When the user cancels before accepting
        chat.response("cancel")
        val repeated = chat.processActionComment(json.fromJsonString())

        // Then no unused live attribute prevents a fresh draft
        repeated.text shouldContain "Draft: hasDiabetes"
        kb.attributeManager.byName("hasDiabetes").shouldBeNull()
        manager.isRuleSessionActive() shouldBe false
    }

    @Test
    fun `a conversation switch makes an old bare acceptance meaningless`() = runTest {
        // Given a draft shown on one case
        chat.startConversation(view)
        chat.processActionComment(json.fromJsonString())

        // When a new conversation starts before acceptance
        chat.startConversation(view)
        chat.response("yes")

        // Then no draft is accepted behind the new conversation
        manager.isRuleSessionActive() shouldBe false
        service.currentAiDefinitionDraft().shouldBeNull()
    }

    @Test
    fun `a caseless draft is retained until a case is selected`() = runTest {
        // Given a KB conversation with no case
        chat.startConversation(null)
        val response = chat.processActionComment(json.fromJsonString())
        val revision = requireNotNull(service.currentAiDefinitionDraft()).definition.revision

        // When a case is selected and the displayed draft is accepted
        response.text shouldContain "Select a case"
        chat.startConversation(view)
        chat.response("yes")
        service.commitCurrentRuleSession()

        // Then the original definition survives selection and is evaluated just once
        val target = requireNotNull(kb.attributeManager.byName("hasDiabetes"))
        (kb.derivedDefinitionManager.definitionFor(target.id) as AiBoolean).revision shouldBe revision
        calls shouldBe 1
    }

    @Test
    fun `accepting a source correction presents a draft before accepting the definition`() = runTest {
        // Given a complete specification except for a misspelt source
        chat.startConversation(view)
        val correction = chat.processActionComment(json.replace("clinical_notes", "clinical_note").fromJsonString())
        correction.text shouldContain "clinical_notes"

        // When the expert accepts the server's source correction
        val response = chat.response("yes")

        // Then the original criteria and labels are preserved in a draft, with a separate acceptance
        response.text shouldContain "Draft: hasDiabetes"
        response.text shouldContain "FH diab → false"
        response.text shouldContain "Preview: true"
        manager.isRuleSessionActive() shouldBe false
        chat.response("yes")
        manager.isRuleSessionActive() shouldBe true
        coVerify(exactly = 0) { model.response("yes") }
    }

    @Test
    fun `explicit retry recovers a failed draft without creating a new revision`() = runTest {
        // Given a draft whose preview could not be evaluated
        chat.startConversation(view)
        fail = true
        val response = chat.processActionComment(json.fromJsonString())
        response.text shouldContain "Could not evaluate"
        val draft = requireNotNull(service.currentAiDefinitionDraft())

        // When the provider recovers and the user explicitly retries
        fail = false
        val retried = chat.processActionComment(ActionComment("RetryAiEvaluation"))

        // Then the cooldown is bypassed for this retry and the draft itself is unchanged
        retried.text shouldContain "Preview: true"
        service.currentAiDefinitionDraft() shouldBe draft
        calls shouldBe 2
        chat.response("yes")
        manager.isRuleSessionActive() shouldBe true
    }

    @Test
    fun `explaining a saved value does not reclassify it`() = runTest {
        // Given an accepted and committed classification
        chat.startConversation(view)
        chat.processActionComment(json.fromJsonString())
        chat.response("yes")
        service.commitCurrentRuleSession()
        val evaluated = kb.viewableCase(case)
        chat.startConversation(evaluated)

        // When the user requests its explanation
        val explanation = chat.processActionComment(ActionComment("ExplainAiEvaluation", attributeName = "hasDiabetes"))

        // Then the saved criteria and excerpt are shown with no new provider call
        explanation.text shouldContain "T2 Diab"
        explanation.text shouldContain "Personal history"
        val explanationView = requireNotNull(explanation.draft)
        explanationView.attributeName shouldBe "hasDiabetes"
        explanationView.source shouldBe "clinical_notes"
        explanationView.previewValue shouldBe "true"
        explanationView.evidence shouldBe "T2 Diab"
        explanationView.valueLabel shouldBe "Value"
        explanationView.prompt.shouldBeNull()
        calls shouldBe 1
    }

    @Test
    fun `explaining a case without a saved result never classifies its notes`() = runTest {
        // Given a definition committed using another case's notes
        chat.startConversation(view)
        chat.processActionComment(json.fromJsonString())
        chat.response("yes")
        service.commitCurrentRuleSession()
        val other = kb.addProcessedCase(RDRCaseBuilder().apply {
            addValue(notes, defaultDate, "Previously unseen notes")
        }.build("Other"))

        // When an explanation is requested without evaluating the new input
        val response = chatFor(kb.viewableCaseOf(other)).processActionComment(
            ActionComment("ExplainAiEvaluation", attributeName = "hasDiabetes"))

        // Then the absence of a saved result is explicit and no new classifier request was made
        response.text shouldContain "No saved evaluation"
        response.draft.shouldBeNull()
        calls shouldBe 1
    }

    @Test
    fun `retry also supports a committed definition after an interpretation failure`() = runTest {
        // Given a committed definition and an unseen case whose interpretation failed
        chat.startConversation(view)
        chat.processActionComment(json.fromJsonString())
        chat.response("yes")
        service.commitCurrentRuleSession()
        val other = kb.addProcessedCase(RDRCaseBuilder().apply {
            addValue(notes, defaultDate, "T2 Diab, reviewed today")
        }.build("Other"))
        val otherChat = chatFor(kb.viewableCaseOf(other))
        fail = true
        otherChat.processActionComment(ActionComment("RetryAiEvaluation")).text shouldContain "Provider unavailable"

        // When an explicit retry follows recovery
        fail = false
        val response = otherChat.processActionComment(ActionComment("RetryAiEvaluation"))

        // Then the existing definition is evaluated and no draft is required
        response.text shouldContain "hasDiabetes: true"
        service.currentAiDefinitionDraft().shouldBeNull()
        calls shouldBe 3
    }

    @Test
    fun `a misrouted explanation request is corrected within the conversation and shows the explanation card`() = runTest {
        // Given an accepted and committed classification with a saved evaluation for the case
        chat.startConversation(view)
        chat.processActionComment(json.fromJsonString())
        chat.response("yes")
        service.commitCurrentRuleSession()

        // And a conversation whose model misroutes the explanation request into the reason
        // transformer, then replies with the action JSON embedded in the correction it is sent
        val sent = mutableListOf<String>()
        val scriptedChat = mockk<Chat>()
        every { scriptedChat.sendMessage(capture(sent)) } answers {
            if (sent.size == 1) functionCallResponse(TRANSFORM_REASON, mapOf(REASON_PARAMETER to "explain hasDiabetes"))
            else textResponse(sent.last().substringAfter("JSON object ").substringBefore(" and nothing else"))
        }
        val transformer = mockk<ReasonTransformer>()
        val conversation = Conversation(
            object : ChatService {
                override fun startChat() = scriptedChat
            },
            mapOf(TRANSFORM_REASON to ReasonTransformHandler(transformer, service))
        )
        val modelChat = ChatManager(conversation, service, mockk())
        modelChat.startConversation(kb.viewableCase(case))

        // When the user asks for the explanation
        val response = modelChat.response("explain hasDiabetes")

        // Then the correction was handed back to the model and the emitted action showed the card
        sent.size shouldBe 2
        sent.last() shouldContain "Function results:"
        sent.last() shouldContain EXPLAIN_AI_EVALUATION
        response.text shouldContain "T2 Diab"
        val card = requireNotNull(response.draft)
        card.attributeName shouldBe "hasDiabetes"
        card.evidence shouldBe "T2 Diab"
        card.previewValue shouldBe "true"
        calls shouldBe 1
        coVerify(exactly = 0) { transformer.transform(any()) }
    }

    @Test
    fun `the production factory supplies authoring services in a KB without a selected case`() {
        // Given a real endpoint and a KB-only chat context
        val kbService = mockk<KnowledgeBaseService>()
        every { kbService.knowledgeBases() } returns listOf(kb.kbInfo)
        every { kbService.demonstrations() } returns emptyList()
        val endpoint = KBEndpoint(session, classifier = classifier)

        // When the production factory constructs the chat (without starting the model)
        val created = ChatManagerFactory { kbService }.create(UserId("expert"), ChatContext.KnowledgeBaseOnly(endpoint))

        // Then the draft is retained in the user's KB session
        val rules = requireNotNull(created.ruleService)
        val draft = rules.proposeAiDefinition("flag", "clinical_notes", "Personal history", emptyList(), null)
        service.currentAiDefinitionDraft() shouldBe draft
        calls shouldBe 0
    }

    private suspend fun chatFor(view: io.rippledown.model.caseview.ViewableCase): ChatManager =
        ChatManager(model, service, mockk()).also { it.startConversation(view) }

    private fun functionCallResponse(name: String, args: Map<String, Any>): GenerateContentResponse {
        val call = mockk<FunctionCall>()
        every { call.name() } returns Optional.of(name)
        every { call.args() } returns Optional.of(args)
        return mockk<GenerateContentResponse>().also {
            every { it.functionCalls() } returns ImmutableList.of(call)
            every { it.text() } returns null
            every { it.usageMetadata() } returns Optional.empty()
        }
    }

    private fun textResponse(text: String): GenerateContentResponse = mockk<GenerateContentResponse>().also {
        every { it.functionCalls() } returns null
        every { it.text() } returns text
        every { it.usageMetadata() } returns Optional.empty()
    }
}
