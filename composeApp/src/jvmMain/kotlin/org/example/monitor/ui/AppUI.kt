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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.example.monitor.core.AdministradorProcesos
import org.example.monitor.modelo.DataProcesos
import java.text.DecimalFormat

@Composable
fun AppUI(manager: AdministradorProcesos) {
    val scope = rememberCoroutineScope()
    var lista by remember { mutableStateOf<List<DataProcesos>>(emptyList()) }
    var filtroProceso by remember { mutableStateOf("") }
    var filtroUsuario by remember { mutableStateOf("") }
    var filtroEstado by remember { mutableStateOf("Todos") }
    var mensaje by remember { mutableStateOf("") }
    var mensajeColor by remember { mutableStateOf(Color.Black) }
    var seleccionados by remember { mutableStateOf(setOf<Int>()) }
    var totalCPU by remember { mutableStateOf(0.0) }
    var totalMem by remember { mutableStateOf(0.0) }
    var expandedEstado by remember { mutableStateOf(false) }

    val estadosDisponibles = listOf(
        "Todos",
        "Running", "Active", "Service", "System", "Background", "Not Responding",
        "Sleeping", "Zombie", "Stopped", "Disk Sleep", "Idle"
    )

    LaunchedEffect(Unit) {
        manager.listProcesses()
        manager.getCpuTotalPorcentaje()

        kotlinx.coroutines.delay(2000)

        lista = manager.listProcesses()
        totalMem = manager.getMemoriaTotalPorcentaje()
        totalCPU = manager.getCpuTotalPorcentaje()
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {

        Text("Monitor de Procesos Sencillo", style = MaterialTheme.typography.h6)
        Text("Creditos: Saúl Fernández Torres (Programador) | Curso: 2ºDAM | Clase: PSP")

        Spacer(Modifier.height(16.dp))

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

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = filtroProceso,
                onValueChange = { filtroProceso = it },
                label = { Text("Proceso") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            OutlinedTextField(
                value = filtroUsuario,
                onValueChange = { filtroUsuario = it },
                label = { Text("Usuario") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )

            Box(modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expandedEstado = !expandedEstado }
                ) {
                    OutlinedTextField(
                        value = filtroEstado,
                        onValueChange = { },
                        label = { Text("Estado") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expandedEstado = !expandedEstado },
                        readOnly = true,
                        enabled = false,
                        trailingIcon = {
                            Text(
                                if (expandedEstado) "▲" else "▼",
                                fontSize = 12.sp,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                        },
                        singleLine = true,
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            disabledTextColor = MaterialTheme.colors.onSurface,
                            disabledBorderColor = MaterialTheme.colors.onSurface.copy(alpha = 0.42f),
                            disabledLabelColor = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                            disabledTrailingIconColor = MaterialTheme.colors.onSurface
                        )
                    )
                }

                DropdownMenu(
                    expanded = expandedEstado,
                    onDismissRequest = { expandedEstado = false }
                ) {
                    estadosDisponibles.forEach { estado ->
                        DropdownMenuItem(onClick = {
                            filtroEstado = estado
                            expandedEstado = false
                        }) {
                            Text(estado)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            var cargando by remember { mutableStateOf(false) }

            Button(
                onClick = {
                    cargando = true
                    mensaje = "Actualizando..."
                    mensajeColor = Color.Black
                    scope.launch {
                        val nuevaLista = manager.listProcesses()
                        lista = nuevaLista

                        totalCPU = manager.getCpuTotalPorcentaje()
                        totalMem = manager.getMemoriaTotalPorcentaje()
                        mensaje = "Lista actualizada correctamente. Procesos: ${nuevaLista.size}"
                        mensajeColor = Color(0xFF00C853)
                        cargando = false
                    }
                },
                enabled = !cargando
            ) {
                Text(if (cargando) "Actualizando..." else "Refrescar")
            }

            Button(
                onClick = {
                    filtroProceso = ""
                    filtroUsuario = ""
                    filtroEstado = "Todos"
                },
                colors = ButtonDefaults.buttonColors(backgroundColor = Color.Yellow)
            ) {
                Text("Limpiar filtros", color = Color.Black)
            }
        }

        Spacer(Modifier.height(16.dp))

        Row(
            Modifier
                .fillMaxWidth()
                .background(Color.LightGray)
                .padding(vertical = 6.dp)
        ) {
            HeaderCell("PID", 60.dp)
            HeaderCell("Nombre", 150.dp)
            HeaderCell("Usuario", 100.dp)
            HeaderCell("CPU (%)", 80.dp)
            HeaderCell("MEM (MB)", 100.dp)
            HeaderCell("Estado", 90.dp)
        }

        Divider()

        val filtrada = lista.filter {
            (filtroProceso.isBlank() || it.nombre.contains(filtroProceso, true)) &&
                    (filtroUsuario.isBlank() || (it.usuario?.contains(filtroUsuario, true) == true)) &&
                    (filtroEstado == "Todos" || (it.estado?.equals(filtroEstado, ignoreCase = true) == true))
        }

        LazyColumn(Modifier.weight(1f)) {
            items(filtrada) { proc ->
                val seleccionado = seleccionados.contains(proc.pid)
                val colorEstado = when (proc.estado) {
                    "Running" -> Color(0xFF00C853)
                    "Active" -> Color(0xFF4CAF50)
                    "Service" -> Color(0xFF42A5F5)
                    "System" -> Color(0xFF9C27B0)
                    "Background" -> Color(0xFF757575)
                    "Not Responding" -> Color(0xFFE53935)
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
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DataCell(proc.pid.toString(), 60.dp)
                    DataCell(proc.nombre, 150.dp)
                    DataCell(proc.usuario ?: "N/A", 100.dp)
                    DataCell(DecimalFormat("#.##").format(proc.cpu ?: 0.0) + "%", 80.dp)
                    DataCell(DecimalFormat("#.##").format(proc.memoria ?: 0.0) + " MB", 100.dp)
                    DataCell(proc.estado ?: "?", 90.dp, color = colorEstado)
                }
                Divider()
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    if (seleccionados.isEmpty()) {
                        mensaje = "⚠️ Selecciona al menos un proceso."
                        mensajeColor = Color(0xFFFF9800)
                    } else {
                        scope.launch {
                            mensaje = "Finalizando procesos..."
                            mensajeColor = Color.Black

                            var exitosos = 0
                            var fallidos = 0
                            val errores = mutableListOf<String>()

                            seleccionados.forEach { pid ->
                                when (val resultado = manager.killProcess(pid)) {
                                    is AdministradorProcesos.KillResult.Success -> {
                                        exitosos++
                                    }
                                    is AdministradorProcesos.KillResult.PermissionDenied -> {
                                        fallidos++
                                        errores.add("PID $pid: ${resultado.msg}")
                                    }
                                    is AdministradorProcesos.KillResult.ProcessNotFound -> {
                                        fallidos++
                                        errores.add("PID $pid: ${resultado.msg}")
                                    }
                                    is AdministradorProcesos.KillResult.Error -> {
                                        fallidos++
                                        errores.add("PID $pid: ${resultado.msg}")
                                    }
                                }
                            }

                            lista = manager.listProcesses()
                            totalCPU = manager.getCpuTotalPorcentaje()
                            totalMem = manager.getMemoriaTotalPorcentaje()

                            mensaje = when {
                                exitosos > 0 && fallidos == 0 -> {
                                    mensajeColor = Color(0xFF00C853)
                                    "✅ $exitosos proceso(s) finalizados correctamente"
                                }
                                exitosos == 0 && fallidos > 0 -> {
                                    mensajeColor = Color(0xFFE53935)
                                    "❌ Error: ${errores.firstOrNull() ?: "No se pudo finalizar ningún proceso"}"
                                }
                                else -> {
                                    mensajeColor = Color(0xFFFF9800)
                                    "⚠️ $exitosos finalizados, $fallidos fallidos. ${errores.firstOrNull() ?: ""}"
                                }
                            }

                            seleccionados = emptySet()
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFD32F2F))
            ) {
                Text("Finalizar", color = Color.White)
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(mensaje, color = mensajeColor, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun HeaderCell(text: String, width: Dp) {
    Text(
        text,
        modifier = Modifier.width(width).padding(start = 6.dp),
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun DataCell(text: String, width: Dp, color: Color = Color.Unspecified) {
    Text(text, modifier = Modifier.width(width).padding(start = 6.dp), color = color)
}