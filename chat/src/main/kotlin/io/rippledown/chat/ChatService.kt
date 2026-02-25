package io.rippledown.chat

import com.google.genai.Chat
import com.google.genai.types.FunctionDeclaration
import io.rippledown.llm.GEMINI_MODEL
import io.rippledown.llm.geminiClient
import io.rippledown.llm.generateContentConfig

interface ChatService {
    /**
     * Starts a new chat session with the model, allowing for an ongoing conversation.
     *
     * @return A chat instance that can be used to send messages and receive responses.
     */
    fun startChat(): Chat
}

/**
 * Service for Gemini Generative AI operations that can interact with text.
 */
class GeminiChatService(systemInstruction: String, functionDeclarations: List<FunctionDeclaration> = emptyList()) :
    ChatService {

    private val config = generateContentConfig(
        systemInstruction = systemInstruction,
        functionDeclarations = functionDeclarations
    )

    override fun startChat(): Chat = geminiClient.chats.create(GEMINI_MODEL, config)
}
