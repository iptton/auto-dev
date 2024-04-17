package cc.unitmesh.devti.llms

import cc.unitmesh.devti.llms.xianghuo.SparkProvider
import cc.unitmesh.devti.settings.AIEngines
import cc.unitmesh.devti.settings.AutoDevSettingsState
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project

@Service
class LlmFactory {
    private val aiEngine: AIEngines
        get() = AIEngines.values()
            .find { it.name.lowercase() == AutoDevSettingsState.getInstance().aiEngine.lowercase() } ?: AIEngines.OpenAI

    fun create(project: Project): LLMProvider2 {
        return when (aiEngine) {
//            AIEngines.OpenAI -> project.getService(OpenAIProvider::class.java)
//            AIEngines.Custom -> project.getService(CustomLLMProvider::class.java)
//            AIEngines.Azure -> project.getService(AzureOpenAIProvider::class.java)
            AIEngines.XingHuo -> project.getService(SparkProvider::class.java)
            else -> project.getService(SparkProvider::class.java)
        }
    }
}
