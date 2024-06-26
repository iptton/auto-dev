package cc.unitmesh.database.flow

import cc.unitmesh.database.DbContextActionProvider
import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.flow.TaskFlow
import cc.unitmesh.devti.gui.chat.ChatCodingPanel
import cc.unitmesh.devti.gui.chat.ChatRole
import cc.unitmesh.devti.llms.ChatMessage
import cc.unitmesh.devti.llms.ChatSession
import cc.unitmesh.devti.llms.LLMProvider2
import cc.unitmesh.devti.template.TemplateRender
import com.intellij.openapi.diagnostic.logger
import kotlinx.coroutines.runBlocking

class AutoSqlFlow(
    private val genSqlContext: AutoSqlContext,
    private val actions: DbContextActionProvider,
    private val panel: ChatCodingPanel,
    private val llm: LLMProvider2
) : TaskFlow<String> {
    private val logger = logger<AutoSqlFlow>()

    override fun clarify(): String {
        val stepOnePrompt = generateStepOnePrompt(genSqlContext, actions)
        val chatSession = ChatSession(conversionName = "AutoSqlFlow", chatHistory = listOf(
            ChatMessage(content = stepOnePrompt, role = ChatRole.User)
        ))

        panel.addMessage(stepOnePrompt, true, stepOnePrompt)
        panel.addMessage(AutoDevBundle.message("autodev.loading"))

        return runBlocking {
            llm.textComplete(chatSession).collect {
                // 只需更新最后一条消息
            }
        }
    }

    override fun design(context: Any): List<String> {
        val tableNames = context as List<String>
        val stepTwoPrompt = generateStepTwoPrompt(genSqlContext, actions, tableNames)

        panel.addMessage(stepTwoPrompt, true, stepTwoPrompt)
        panel.addMessage(AutoDevBundle.message("autodev.loading"))

        return runBlocking {
            val prompt = llm.stream(stepTwoPrompt, "")
            return@runBlocking panel.updateMessage(prompt)
        }.let { listOf(it) }
    }

    private fun generateStepOnePrompt(context: AutoSqlContext, actions: DbContextActionProvider): String {
        val templateRender = TemplateRender("genius/sql")
        val template = templateRender.getTemplate("sql-gen-clarify.vm")

        templateRender.context = context
        templateRender.actions = actions

        val prompter = templateRender.renderTemplate(template)

        logger.info("Prompt: $prompter")
        return prompter
    }

    private fun generateStepTwoPrompt(
        genSqlContext: AutoSqlContext,
        actions: DbContextActionProvider,
        tableInfos: List<String>
    ): String {
        val templateRender = TemplateRender("genius/sql")
        val template = templateRender.getTemplate("sql-gen-design.vm")

        genSqlContext.tableInfos = actions.getTableColumns(tableInfos)

        templateRender.context = genSqlContext
        templateRender.actions = actions

        val prompter = templateRender.renderTemplate(template)

        logger.info("Prompt: $prompter")
        return prompter
    }

    fun getAllTables(): List<String> {
        return actions.dasTables.map { it.name }
    }
}
