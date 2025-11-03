package org.example.monitor

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import org.example.monitor.core.AdministradorProcesos
import org.example.monitor.ui.AppUI

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "Monitor de Procesos PSP") {
        MaterialTheme {
            AppRoot()
        }
    }
}

@Composable
fun AppRoot() {
    val manager = AdministradorProcesos()
    AppUI(manager)
}
