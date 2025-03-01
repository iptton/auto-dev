package cc.unitmesh.devti.gui.compose

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.intellij.ui.JBColor

@Composable
fun IDEATheme(
    content: @Composable () -> Unit
) {
    val colorScheme = if (JBColor.isBright()) {
        lightColorScheme(
            primary = Color(JBColor.namedColor("Button.startBackground", 0x4C5052).rgb),
            surface = Color(JBColor.background().rgb),
            background = Color(JBColor.background().rgb),
            onSurface = Color(JBColor.foreground().rgb),
            onBackground = Color(JBColor.foreground().rgb),
            error = Color(JBColor.RED.rgb),
            onError = Color(JBColor.WHITE.rgb)
        )
    } else {
        darkColorScheme(
            primary = Color(JBColor.namedColor("Button.startBackground", 0x4C5052).rgb),
            surface = Color(JBColor.background().rgb),
            background = Color(JBColor.background().rgb),
            onSurface = Color(JBColor.foreground().rgb),
            onBackground = Color(JBColor.foreground().rgb),
            error = Color(JBColor.RED.rgb),
            onError = Color(JBColor.WHITE.rgb)
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
