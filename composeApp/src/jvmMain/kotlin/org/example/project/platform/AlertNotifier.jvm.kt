package org.example.project.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import javax.swing.JOptionPane
import javax.swing.SwingUtilities

@Composable
actual fun rememberAlertNotifier(): AlertNotifier {
    return remember {
        object : AlertNotifier {
            override fun notifyPriceAlert(title: String, message: String) {
                SwingUtilities.invokeLater {
                    JOptionPane.showMessageDialog(
                        null,
                        message,
                        title,
                        JOptionPane.INFORMATION_MESSAGE
                    )
                }
            }

            override fun beep() {
                java.awt.Toolkit.getDefaultToolkit().beep()
            }
        }
    }
}