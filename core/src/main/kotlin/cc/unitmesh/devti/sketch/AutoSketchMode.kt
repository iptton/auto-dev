package cc.unitmesh.devti.sketch

import cc.unitmesh.devti.devin.dataprovider.BuiltinCommand
import cc.unitmesh.devti.provider.toolchain.ToolchainFunctionProvider
import cc.unitmesh.devti.util.parser.CodeFence
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
class AutoSketchMode() {
    var isEnable: Boolean = false

    fun start(text: String, listener: SketchInputListener) {
        val codeFenceList = CodeFence.parseAll(text)
        val devinCodeFence = codeFenceList.filter { it.language.displayName == "DevIn" }

        val allCode = devinCodeFence.filter {
            !it.text.contains("<DevinsError>") && (hasReadCommand(it) || hasToolchainFunctionCommand(it))
        }

        if (allCode.isEmpty()) return

        val allCodeText = allCode.map { it.text }.distinct().joinToString("\n")
        if (allCodeText.trim().isEmpty()) {
            logger<SketchToolWindow>().error("No code found")
        } else {
            listener.manualSend(allCodeText)
        }
    }

    private fun hasReadCommand(fence: CodeFence): Boolean = BuiltinCommand.READ_COMMANDS.any { command ->
        fence.text.contains("/" + command.commandName + ":")
    }

    private fun hasToolchainFunctionCommand(fence: CodeFence): Boolean {
        val toolchainCmds = ToolchainFunctionProvider.all().map { it.funcNames() }.flatten()
        return toolchainCmds.any {
            fence.text.contains("/$it:")
        }
    }

    companion object {
        fun getInstance(project: Project): AutoSketchMode {
            return project.service<AutoSketchMode>()
        }
    }
}