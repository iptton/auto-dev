package cc.unitmesh.devti.llms.mock

import cc.unitmesh.devti.llms.LLMProvider
import com.intellij.openapi.components.Service
import kotlinx.coroutines.flow.Flow

@Service(Service.Level.PROJECT)
class MockProvider: LLMProvider {
    override fun stream(promptText: String, systemPrompt: String, keepHistory: Boolean): Flow<String> {
        TODO("Not yet implemented")
    }
}
