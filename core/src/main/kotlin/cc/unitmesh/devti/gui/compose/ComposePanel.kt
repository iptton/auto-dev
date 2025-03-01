package cc.unitmesh.devti.gui.compose

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposePanel
import androidx.compose.ui.unit.dp
import java.awt.Dimension
import javax.swing.JComponent

/**
 * 创建一个Compose UI面板，可以集成到IntelliJ IDEA的Swing界面中
 */
class AutoDevComposePanel {
    /**
     * 创建一个Compose UI组件
     * @return 返回一个可以集成到IntelliJ IDEA的JComponent
     */
    fun createComposePanel(): JComponent {
        return ComposePanel().apply {
            preferredSize = parent.size
            addHierarchyBoundsListener(object : java.awt.event.HierarchyBoundsListener {
                override fun ancestorMoved(e: java.awt.event.HierarchyEvent?) {}
                override fun ancestorResized(e: java.awt.event.HierarchyEvent?) {
                    size = parent?.size ?: size
                }
            })
            setContent {
                IDEATheme {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        ComposeContent()
                    }
                }
            }
        }
    }
}

/**
 * Compose UI内容组件
 */
@Composable
fun ComposeContent() {
    var text by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "AutoDev Compose UI",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            label = { Text("输入内容") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = { text = "" }) {
            Text("清除")
        }
    }
}