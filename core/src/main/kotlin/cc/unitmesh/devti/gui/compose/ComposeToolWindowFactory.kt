package cc.unitmesh.devti.gui.compose

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory

/**
 * Compose工具窗口工厂类
 * 用于创建一个使用Compose UI的工具窗口
 */
class ComposeToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val composePanel = AutoDevComposePanel()
        val content = ContentFactory.getInstance().createContent(
            composePanel.createComposePanel(),
            "Compose UI",
            false
        )
        
        toolWindow.contentManager.addContent(content)
    }
}