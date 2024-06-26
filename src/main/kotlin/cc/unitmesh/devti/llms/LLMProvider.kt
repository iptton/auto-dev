package cc.unitmesh.devti.llms

import cc.unitmesh.devti.gui.chat.ChatRole
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

interface LLMProvider {
    val defaultTimeout: Long get() = 600

    fun stream(promptText: String, systemPrompt: String, keepHistory: Boolean = true): Flow<String>

    fun clearMessage() {

    }

    fun appendLocalMessage(msg: String, role: ChatRole) {}
}
