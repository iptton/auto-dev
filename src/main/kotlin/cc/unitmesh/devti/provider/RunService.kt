package cc.unitmesh.devti.provider

import com.intellij.execution.Executor
import com.intellij.execution.ExecutorRegistryImpl
import com.intellij.execution.RunManager
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import java.util.function.Consumer

val RW_MAIN_CONFIGURATION_ID: Key<String> = Key<String>("RunWidgetMainRunConfigurationId")

interface RunService {
    private val logger: Logger get() = logger<RunService>()

    /**
     * Retrieves the run configuration class for the given project.
     *
     * @param project The project for which to retrieve the run configuration class.
     * @return The run configuration class for the project.
     */
    fun runConfigurationClass(project: Project): Class<out RunProfile>?

    fun createConfiguration(project: Project, virtualFile: VirtualFile): RunConfiguration? = null

    fun createConfiguration(project: Project, path: String): RunConfiguration? = null

    fun runFile(project: Project, virtualFile: VirtualFile) {
        val runManager = RunManager.getInstance(project)
        var testConfig = runManager.allConfigurationsList.firstOrNull {
            val runConfigureClass = runConfigurationClass(project)
            it.name == virtualFile.nameWithoutExtension && (it.javaClass == runConfigureClass)
        }

        var isTemporary = false

        // try to create config if not founds
        if (testConfig == null) {
            isTemporary = true
            testConfig = createConfiguration(project, virtualFile)
        }

        if (testConfig == null) {
            logger.warn("Failed to find test configuration for: ${virtualFile.nameWithoutExtension}")
            return
        }

        val settings = runManager.findConfigurationByTypeAndName(testConfig.type, testConfig.name)
        if (settings == null) {
            logger.warn("Failed to find test configuration for: ${virtualFile.nameWithoutExtension}")
            return
        }

        if (isTemporary) {
            settings.isTemporary = true
        }

        logger.info("configurationSettings: $settings")
        runManager.selectedConfiguration = settings

        val executor: Executor = DefaultRunExecutor()
        val dataContext = DataContext.EMPTY_CONTEXT

        ExecutorRegistryImpl.RunnerHelper.runSubProcess(
            project,
            testConfig,
            settings,
            dataContext,
            executor
        ) { ee: ExecutionEnvironment ->
            // TODO: Add customizations for process data
        }
    }
}