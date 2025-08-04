/*
 * MIT License
 *
 * Copyright (c) 2024 Shreyas Patil
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package io.rippledown.chat

import dev.shreyaspatil.ai.client.generativeai.Chat
import dev.shreyaspatil.ai.client.generativeai.type.Content
import dev.shreyaspatil.ai.client.generativeai.type.FunctionDeclaration
import io.rippledown.llm.generativeModel

interface ChatService {
    /**
     * Starts a new chat session with the model, allowing for an ongoing conversation.
     *
     * @param history Optional initial conversation history to start the chat with.
     * @return A chat instance that can be used to send messages and receive responses.
     */
    fun startChat(history: List<Content> = emptyList()): Chat
}

/**
 * Service for Gemini Generative AI operations that can interact with text.
 *
 * Acknowledgement: This code is based on the work of Shreyas Patil
 * @see <a href="https://github.com/PatilShreyas/ChaKt-KMP">ChaKt-KMP</a>
 */
class GeminiChatService(systemInstruction: String, functionDeclarations: List<FunctionDeclaration> = emptyList()) :
    ChatService {

    private val model = generativeModel(
        systemInstruction = systemInstruction,
        functionDeclarations = functionDeclarations
    )

    override fun startChat(history: List<Content>) = model.startChat(history)
}
