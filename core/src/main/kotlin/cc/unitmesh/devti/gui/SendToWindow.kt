package cc.unitmesh.devti.gui

import cc.unitmesh.devti.gui.chat.ChatCodingService
import cc.unitmesh.devti.gui.chat.NormalChatCodingPanel
import cc.unitmesh.devti.gui.chat.message.ChatActionType
import cc.unitmesh.devti.provider.ContextPrompter
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project

fun sendToChatWindow(
    project: Project,
    actionType: ChatActionType,
    runnable: (NormalChatCodingPanel, ChatCodingService) -> Unit,
) {
    val chatCodingService = ChatCodingService(actionType, project)

    val toolWindowManager = AutoDevToolWindowFactory.getToolWindow(project) ?: run {
        logger<ChatCodingService>().warn("Tool window not found")
        return
    }

    val contentPanel = AutoDevToolWindowFactory.labelNormalChat(chatCodingService) ?: run {
        logger<ChatCodingService>().warn("Content panel not found")
        return
    }

    toolWindowManager.activate {
        runnable(contentPanel, chatCodingService)
    }
}

fun sendToChatPanel(project: Project, runnable: (NormalChatCodingPanel, ChatCodingService) -> Unit) {
    val actionType = ChatActionType.CHAT
    sendToChatWindow(project, actionType, runnable)
}

fun sendToChatPanel(project: Project, actionType: ChatActionType, runnable: (NormalChatCodingPanel, ChatCodingService) -> Unit) {
    sendToChatWindow(project, actionType, runnable)
}

fun sendToChatPanel(project: Project, actionType: ChatActionType, prompter: ContextPrompter) {
    sendToChatWindow(project, actionType) { contentPanel, chatCodingService ->
        chatCodingService.handlePromptAndResponse(contentPanel, prompter, keepHistory = true)
    }
}
