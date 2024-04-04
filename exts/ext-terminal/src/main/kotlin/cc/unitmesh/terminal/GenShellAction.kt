// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package cc.unitmesh.terminal

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformCoreDataKeys
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.JBPopupListener
import com.intellij.openapi.ui.popup.LightweightWindowEvent
import com.intellij.openapi.wm.IdeFocusManager
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.StartupUiUtil
import com.intellij.util.ui.SwingHelper
import org.jetbrains.plugins.terminal.TerminalToolWindowManager
import java.awt.Component
import java.awt.Font
import java.awt.Point
import java.awt.event.FocusAdapter
import java.awt.event.FocusEvent
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import javax.swing.Box
import javax.swing.JTextField
import javax.swing.event.DocumentEvent

private const val OUTLINE_PROPERTY = "JComponent.outline"
private const val ERROR_VALUE = "error"


class GenShellAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val contextComponent = e.getData(PlatformCoreDataKeys.CONTEXT_COMPONENT) ?: return
        TerminalToolWindowManager.getInstance(project).toolWindow
        showContentRenamePopup(project, contextComponent)
    }

    fun showContentRenamePopup(project: Project, contextComponent: Component) {
        val textField = JTextField()
        textField.selectAll()

        val label = JBLabel()
        label.font = StartupUiUtil.labelFont.deriveFont(Font.BOLD)

        val panel = SwingHelper.newLeftAlignedVerticalPanel(label, Box.createVerticalStrut(JBUI.scale(2)), textField)
        panel.addFocusListener(object : FocusAdapter() {
            override fun focusGained(e: FocusEvent?) {
                IdeFocusManager.findInstance().requestFocus(textField, false)
            }
        })

        val balloon = JBPopupFactory.getInstance().createDialogBalloonBuilder(panel, null)
            .setShowCallout(true)
            .setCloseButtonEnabled(false)
            .setAnimationCycle(0)
            .setHideOnKeyOutside(true)
            .setHideOnClickOutside(true)
            .setRequestFocus(true)
            .setBlockClicksThroughBalloon(true)
            .createBalloon()

        textField.addKeyListener(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent?) {
                if (e != null && e.keyCode == KeyEvent.VK_ENTER) {
                    if (textField.text.isEmpty()) {
                        textField.putClientProperty(OUTLINE_PROPERTY, ERROR_VALUE)
                        textField.repaint()
                        return
                    }
//                    applyScript(content, project, textField.text)
                    balloon.hide()
                }
            }
        })

        textField.document.addDocumentListener(object : DocumentAdapter() {
            override fun textChanged(e: DocumentEvent) {
                val outlineValue = textField.getClientProperty(OUTLINE_PROPERTY)
                if (outlineValue == ERROR_VALUE) {
                    textField.putClientProperty(OUTLINE_PROPERTY, null)
                    textField.repaint()
                }
            }
        })

        balloon.show(RelativePoint(contextComponent, Point(400, 0)), Balloon.Position.above)
        balloon.addListener(object : JBPopupListener {
            override fun onClosed(event: LightweightWindowEvent) {
                IdeFocusManager.findInstance().requestFocus(contextComponent, false)
            }
        })
    }
}