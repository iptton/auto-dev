package cc.unitmesh.devti.sketch

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.alignRight
import cc.unitmesh.devti.gui.chat.ChatCodingService
import cc.unitmesh.devti.gui.chat.message.ChatActionType
import cc.unitmesh.devti.gui.chat.ui.AutoDevInputSection
import cc.unitmesh.devti.gui.chat.view.MessageView
import cc.unitmesh.devti.gui.toolbar.NewSketchAction
import cc.unitmesh.devti.inline.AutoDevInlineChatService
import cc.unitmesh.devti.inline.fullHeight
import cc.unitmesh.devti.inline.fullWidth
import cc.unitmesh.devti.sketch.ui.ExtensionLangSketch
import cc.unitmesh.devti.sketch.ui.LangSketch
import cc.unitmesh.devti.sketch.ui.LanguageSketchProvider
import cc.unitmesh.devti.sketch.ui.MarkdownPreviewHighlightSketch
import cc.unitmesh.devti.sketch.ui.code.CodeHighlightSketch
import cc.unitmesh.devti.util.AutoDevCoroutineScope
import cc.unitmesh.devti.util.parser.CodeFence
import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.impl.ActionButton
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileTypes.PlainTextLanguage
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.NullableComponent
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.panels.VerticalLayout
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.Row
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import kotlinx.coroutines.launch
import org.jetbrains.annotations.NonNls
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*

interface SketchProcessListener {
    fun onBefore() {}
    fun onAfter() {}
}

open class SketchToolWindow(
    val project: Project,
    open val editor: Editor?,
    private val showInput: Boolean = false,
    chatActionType: ChatActionType = ChatActionType.SKETCH
) : SimpleToolWindowPanel(true, true), NullableComponent, Disposable {
    open val chatCodingService = ChatCodingService(chatActionType, project)
    open val inputListener: SketchInputListener = SketchInputListener(project, chatCodingService, this)
    private var progressBar: JProgressBar = JProgressBar()

    private var thinkingHighlight: CodeHighlightSketch =
        CodeHighlightSketch(project, "<Thinking />", PlainTextLanguage.INSTANCE)
    private var thinkingPanel = panel {
        row {
            cell(thinkingHighlight).fullWidth()
        }
    }

    private var inputSection: AutoDevInputSection = AutoDevInputSection(project, this, showAgent = false)

    private var myText: String = ""

    private var myList = JPanel(VerticalLayout(JBUI.scale(0))).apply {
        this.isOpaque = true
    }
    private var historyPanel = JPanel(VerticalLayout(JBUI.scale(0))).apply {
        this.isOpaque = true
    }

    protected var isUserScrolling: Boolean = false
    protected var isInterrupted: Boolean = false

    protected var systemPromptPanel: JPanel = JPanel(BorderLayout())
    protected var contentPanel = JPanel(BorderLayout())

    val header = JButton(AllIcons.Actions.Copy).apply {
        this@apply.preferredSize = Dimension(32, 32)

        addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent?) {
                var allText = chatCodingService.getAllMessages().joinToString("\n") { it.content }
                val selection = StringSelection(allText)
                val clipboard = Toolkit.getDefaultToolkit().systemClipboard
                clipboard.setContents(selection, null)
            }
        })
    }

    protected var panelContent: DialogPanel = panel {
        row { cell(systemPromptPanel).fullWidth().fullHeight() }
        row { cell(header).alignRight() }
        row { cell(historyPanel).fullWidth().fullHeight() }
        row { cell(myList).fullWidth().fullHeight() }
    }

    private val scrollPanel: JBScrollPane = JBScrollPane(
        panelContent,
        ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
        ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
    ).apply {
        this.verticalScrollBar.autoscrolls = true
        this.verticalScrollBar.addAdjustmentListener { e ->
            if (e.valueIsAdjusting) {
                isUserScrolling = true
            }
        }
    }

    var handleCancel: ((String) -> Unit)? = null

    private val processListeners = mutableListOf<SketchProcessListener>()

    init {
        if (showInput) {
            val header = panel {
                row {
                    checkBox(AutoDevBundle.message("sketch.composer.mode")).apply {
                        this.component.addActionListener {
                            AutoSketchMode.getInstance(project).isEnable = this.component.isSelected
                        }
                    }

                    createActionButton(NewSketchAction()).alignRight()
                }
            }

            header.border = JBUI.Borders.compound(
                JBUI.Borders.customLine(UIUtil.getBoundsColor(), 0, 0, 1, 0),
                JBUI.Borders.empty(0, 4)
            )

            contentPanel.add(header, BorderLayout.NORTH)
        }

        contentPanel.add(scrollPanel, BorderLayout.CENTER)
        contentPanel.addKeyListener(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                if (e.keyCode == KeyEvent.VK_ESCAPE) {
                    if (editor != null) {
                        AutoDevInlineChatService.getInstance().closeInlineChat(editor!!)
                    }
                }
            }
        })

        if (showInput) {
            ApplicationManager.getApplication().invokeLater {
                AutoDevCoroutineScope.scope(project).launch {
                    setupListener()
                }
            }
        }

        setContent(contentPanel)
    }

    private suspend fun setupListener() {
        inputSection.also {
            it.border = JBUI.Borders.empty(8)
        }

        inputListener.setup()
        inputSection.addListener(inputListener)

        contentPanel.add(panel {
            row {
                cell(progressBar).fullWidth()
            }
            row {
                cell(thinkingPanel).fullWidth()
            }
            row {
                cell(inputSection).fullWidth()
            }
        }, BorderLayout.SOUTH)

        addProcessListener(object : SketchProcessListener {
            override fun onBefore() {
                isInterrupted = false
                inputSection.showStopButton()
            }

            override fun onAfter() {
                inputSection.showSendButton()
            }
        })
    }

    fun onStart() {
        beforeRun()
        initializePreAllocatedBlocks(project)
        progressBar.isVisible = true
    }

    fun hiddenProgressBar() {
        progressBar.isVisible = false
    }

    fun stop() {
        cancel("Stop")
        inputSection.showSendButton()
    }

    fun addProcessListener(processorListener: SketchProcessListener) {
        processListeners.add(processorListener)
    }

    fun beforeRun() {
        processListeners.forEach { it.onBefore() }
    }

    fun afterRun() {
        processListeners.forEach { it.onAfter() }
    }

    private val blockViews: MutableList<LangSketch> = mutableListOf()
    private fun initializePreAllocatedBlocks(project: Project) {
        repeat(32) {
            runInEdt {
                val codeBlockViewer = CodeHighlightSketch(project, "", PlainTextLanguage.INSTANCE)
                blockViews.add(codeBlockViewer)
                myList.add(codeBlockViewer)
            }
        }
    }

    override fun dispose() {
        chatCodingService.clearSession()
    }

    fun addRequestPrompt(text: String) {
        progressBar.isIndeterminate = true

        runInEdt {
            historyPanel.add(createSingleTextView(text, language = "DevIn"))
            this.revalidate()
            this.repaint()
        }
    }

    fun addSystemPrompt(text: String) {
        runInEdt {
            systemPromptPanel.add(createSingleTextView(text, language = "VTL"))
            this.revalidate()
            this.repaint()
        }
    }

    fun updateHistoryPanel() {
        runInEdt {
            blockViews.filter { it.getViewText().isNotEmpty() }.forEach {
                historyPanel.add(it.getComponent())
            }

            blockViews.clear()
            myList.removeAll()

            this.revalidate()
            this.repaint()
        }
    }

    fun createSingleTextView(text: String, language: String = "markdown"): DialogPanel {
        return MessageView.createSingleTextView(project, text, language)
    }

    fun onUpdate(text: String) {
        myText = text
        val codeFenceList = CodeFence.parseAll(text)

        runInEdt {
            codeFenceList.forEachIndexed { index, codeFence ->
                if (index < blockViews.size) {
                    var langSketch: ExtensionLangSketch? = null
                    if (codeFence.originLanguage != null && codeFence.isComplete && blockViews[index] !is ExtensionLangSketch) {
                        langSketch = LanguageSketchProvider.provide(codeFence.originLanguage)
                            ?.create(project, codeFence.text)
                    }

                    val isCanHtml = codeFence.language.displayName.lowercase() == "markdown"
                    if (isCanHtml && codeFence.isComplete && blockViews[index] !is ExtensionLangSketch) {
                        langSketch = MarkdownPreviewHighlightSketch(project, codeFence.text)
                    }

                    if (langSketch != null) {
                        val oldComponent = blockViews[index]
                        blockViews[index] = langSketch
                        myList.remove(index)
                        myList.add(langSketch.getComponent(), index)

                        oldComponent.dispose()
                    } else {
                        blockViews[index].apply {
                            updateLanguage(codeFence.language, codeFence.originLanguage)
                            updateViewText(codeFence.text, codeFence.isComplete)
                        }
                    }
                } else {
                    val codeBlockViewer = CodeHighlightSketch(project, codeFence.text, PlainTextLanguage.INSTANCE)
                    blockViews.add(codeBlockViewer)
                    myList.add(codeBlockViewer.getComponent())
                }
            }

            while (blockViews.size > codeFenceList.size) {
                val lastIndex = blockViews.lastIndex
                try {
                    blockViews.removeAt(lastIndex)
                    myList.remove(lastIndex)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            myList.revalidate()
            myList.repaint()

            scrollToBottom()
        }
    }

    fun onFinish(text: String) {
        myText = text
        runInEdt {
            blockViews.filter { it.getViewText().isNotEmpty() }.forEach {
                it.onDoneStream(text)
            }

            blockViews.filter { it.getViewText().isEmpty() }.forEach {
                myList.remove(it.getComponent())
            }
        }

        progressBar.isIndeterminate = false
        progressBar.isVisible = false
        scrollToBottom()

        afterRun()

        if (AutoSketchMode.getInstance(project).isEnable && !isInterrupted) {
            AutoSketchMode.getInstance(project).start(text, this@SketchToolWindow.inputListener)
        }
    }

    fun sendInput(text: String) {
        inputSection.text += "\n" + text
        inputSection.send()
    }

    private fun scrollToBottom() {
        if (!isUserScrolling) {
            SwingUtilities.invokeLater {
                val verticalScrollBar = scrollPanel.verticalScrollBar
                verticalScrollBar.value = verticalScrollBar.maximum
            }
        }
    }

    fun resize(maxHeight: Int = 480) {
        val height = myList.components.sumOf { it.height }
        if (height < maxHeight) {
            this.minimumSize = JBUI.size(800, height)
        } else {
            this.minimumSize = JBUI.size(800, maxHeight)
            scrollPanel.minimumSize = JBUI.size(800, maxHeight)
        }
    }

    override fun isNull(): Boolean = !isVisible

    fun cancel(s: String) = runCatching {
        handleCancel?.also { handleCancel = null }?.invoke(s)
        isInterrupted = true
    }

    fun resetSketchSession() {
        chatCodingService.clearSession()
        progressBar.isIndeterminate = false
        progressBar.isVisible = false
        blockViews.clear()
        systemPromptPanel.removeAll()
        myList.removeAll()
        historyPanel.removeAll()
        initializePreAllocatedBlocks(project)
    }

    fun printThinking(string: String) {
        runInEdt {
            thinkingPanel.isVisible = true
            thinkingHighlight.updateViewText(string, false)
        }
    }

    fun hiddenThinking() {
        runInEdt {
            thinkingPanel.isVisible = false
        }
    }
}

fun Row.createActionButton(action: AnAction, @NonNls actionPlace: String = ActionPlaces.UNKNOWN): Cell<ActionButton> {
    val component = ActionButton(
        action,
        action.templatePresentation.clone(),
        actionPlace,
        ActionToolbar.DEFAULT_MINIMUM_BUTTON_SIZE
    )
    return cell(component)
}