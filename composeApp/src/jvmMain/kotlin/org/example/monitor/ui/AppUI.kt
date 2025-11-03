package org.example.monitor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.example.monitor.core.AdministradorProcesos
import org.example.monitor.modelo.DataProcesos
import java.io.File
import java.text.DecimalFormat

@Composable
fun AppUI(manager: AdministradorProcesos) {
    val scope = rememberCoroutineScope()
    var lista by remember { mutableStateOf<List<DataProcesos>>(emptyList()) }
    var filtroProceso by remember { mutableStateOf("") }
    var filtroUsuario by remember { mutableStateOf("") }
    var filtroEstado by remember { mutableStateOf("") }
    var mensaje by remember { mutableStateOf("") }
    var seleccionados by remember { mutableStateOf(setOf<Int>()) }

    // Cargar al inicio
    LaunchedEffect(Unit) { lista = manager.listProcesses() }

    val totalCPU = lista.mapNotNull { it.cpu }.sum()
    val totalMem = lista.mapNotNull { it.memoria }.sum()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {

        Text("Monitor de Procesos", style = MaterialTheme.typography.h6)
        Text("Interfaz moderna • selección múltiple • acciones seguras")

        Spacer(Modifier.height(16.dp))

        // Resumen CPU y Memoria
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Card(
                backgroundColor = Color(0xFF1E1E2F),
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("⚡ CPU total", color = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("${DecimalFormat("#.##").format(totalCPU)}%", color = Color(0xFFffd700))
                }
            }

            Card(
                backgroundColor = Color(0xFF1E1E2F),
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🧠 Memoria", color = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("${DecimalFormat("#.##").format(totalMem)}%", color = Color(0xFFffb347))
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Filtros
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = filtroProceso,
                onValueChange = { filtroProceso = it },
                label = { Text("Proceso") },
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = filtroUsuario,
                onValueChange = { filtroUsuario = it },
                label = { Text("Usuario") },
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = filtroEstado,
                onValueChange = { filtroEstado = it },
                label = { Text("Estado") },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(12.dp))

        // Botones de filtro
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                scope.launch { lista = manager.listProcesses() }
            }) { Text("Refrescar") }

            Button(onClick = {
                filtroProceso = ""
                filtroUsuario = ""
                filtroEstado = ""
            }, colors = ButtonDefaults.buttonColors(backgroundColor = Color.Yellow)) {
                Text("Limpiar filtros", color = Color.Black)
            }
        }

        Spacer(Modifier.height(16.dp))

        // Tabla
        val filtrada = lista.filter {
            (filtroProceso.isBlank() || it.nombre.contains(filtroProceso, true)) &&
                    (filtroUsuario.isBlank() || (it.usuario?.contains(filtroUsuario, true) == true)) &&
                    (filtroEstado.isBlank() || (it.estado?.contains(filtroEstado, true) == true))
        }

        Text("Procesos (${filtrada.size})", style = MaterialTheme.typography.subtitle1)
        Spacer(Modifier.height(8.dp))

        LazyColumn(Modifier.weight(1f)) {
            items(filtrada) { proc ->
                val seleccionado = seleccionados.contains(proc.pid)
                val colorEstado = when (proc.estado) {
                    "Running" -> Color(0xFF00C853)
                    "Sleeping" -> Color(0xFF42A5F5)
                    "Zombie" -> Color(0xFFE53935)
                    else -> Color.LightGray
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (seleccionado) Color(0x221E88E5) else Color.Transparent)
                        .clickable {
                            seleccionados =
                                if (seleccionados.contains(proc.pid)) seleccionados - proc.pid
                                else seleccionados + proc.pid
                        }
                        .padding(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(proc.pid.toString(), Modifier.width(60.dp))
                    Text(proc.nombre, Modifier.width(150.dp))
                    Text(proc.usuario ?: "N/A", Modifier.width(100.dp))
                    Text("${proc.cpu ?: 0.0}%", Modifier.width(80.dp))
                    Text("${proc.memoria ?: 0.0}%", Modifier.width(80.dp))
                    Text(proc.estado ?: "?", color = colorEstado, modifier = Modifier.width(90.dp))
                    Text(proc.comando ?: "", Modifier.weight(1f))
                }
                Divider()
            }
        }

        Spacer(Modifier.height(12.dp))

        // Botones inferiores
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                scope.launch {
                    seleccionados.forEach {
                        manager.killProcess(it)
                    }
                    lista = manager.listProcesses()
                    mensaje = "Procesos finalizados: ${seleccionados.joinToString()}"
                    seleccionados = emptySet()
                }
            }) { Text("Finalizar") }

            Button(onClick = { mensaje = "Detalles próximamente" }) {
                Text("Detalles")
            }

            Button(onClick = {
                val csvFile = File("procesos_export.csv")
                csvFile.writeText("PID,Proceso,Usuario,CPU,MEM,Estado,Comando\n")
                filtrada.forEach {
                    csvFile.appendText("${it.pid},${it.nombre},${it.usuario},${it.cpu},${it.memoria},${it.estado},${it.comando}\n")
                }
                mensaje = "Exportado a ${csvFile.absolutePath}"
            }) { Text("Exportar CSV") }
        }

        Spacer(Modifier.height(8.dp))
        Text(mensaje)
    }
}
