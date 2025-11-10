package org.example.monitor.core

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.example.monitor.modelo.DataProcesos
import java.io.BufferedReader
import java.io.InputStreamReader
import java.lang.management.ManagementFactory
import com.sun.management.OperatingSystemMXBean

class AdministradorProcesos {

    private val os = detectarSO()

    private val cpuTimeCache = mutableMapOf<Int, CpuMeasurement>()

    private val tipoProcesoCache = mutableMapOf<Int, String>()

    data class CpuMeasurement(
        val cpuTime: Double,
        val timestamp: Long
    )

    suspend fun listProcesses(): List<DataProcesos> = withContext(Dispatchers.IO) {
        try {
            when (os) {
                SistemaOperativo.WINDOWS -> listarWindows()
                SistemaOperativo.LINUX, SistemaOperativo.MAC -> listarUnix()
                else -> emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun runCommand(vararg cmd: String): List<String> {
        val pb = ProcessBuilder(*cmd)
        pb.redirectErrorStream(true)
        val process = pb.start()
        val reader = BufferedReader(InputStreamReader(process.inputStream))
        val lines = reader.readLines()
        process.waitFor()
        return lines
    }

    private fun listarWindows(): List<DataProcesos> {
        val lines = runCommand("cmd", "/c", "tasklist /v /fo csv /nh")
        val procesos = mutableListOf<DataProcesos>()

        val currentTime = System.currentTimeMillis()

        for (line in lines) {
            val cols = parseCsv(line)
            if (cols.size < 8) continue

            val nombre = cols[0].trim('"')
            val pid = cols[1].trim('"').toIntOrNull() ?: continue
            val usuario = cols.getOrNull(6)?.trim('"')
            val memStr = cols.getOrNull(4)?.replace("[^0-9]".toRegex(), "") ?: "0"
            val memoriaMB = memStr.toDoubleOrNull()?.div(1024) ?: 0.0
            val estadoRaw = cols.getOrNull(5)?.trim('"') ?: "Unknown"
            val comando = nombre

            val cpuTimeStr = cols.getOrNull(7)?.trim('"') ?: "00:00:00"
            val partes = cpuTimeStr.split(":").map { it.toDoubleOrNull() ?: 0.0 }
            val segundosCpu = when (partes.size) {
                3 -> partes[0] * 3600 + partes[1] * 60 + partes[2]
                2 -> partes[0] * 60 + partes[1]
                else -> 0.0
            }

            val cpuPercent = calcularCpuPorcentaje(pid, segundosCpu, currentTime)

            val estadoReal = when {
                estadoRaw.equals("Running", ignoreCase = true) -> "Running"
                estadoRaw.contains("Not Responding", ignoreCase = true) -> "Not Responding"
                else -> "Unknown"
            }

            procesos.add(DataProcesos(pid, nombre, usuario, cpuPercent, memoriaMB, estadoReal, comando))
        }

        val pidsActuales = procesos.map { it.pid }.toSet()
        cpuTimeCache.keys.retainAll(pidsActuales)
        tipoProcesoCache.keys.retainAll(pidsActuales)

        return procesos
    }


    private fun calcularCpuPorcentaje(pid: Int, segundosCpu: Double, currentTime: Long): Double {
        val previousMeasurement = cpuTimeCache[pid]

        return if (previousMeasurement != null) {
            val deltaCpuSeconds = segundosCpu - previousMeasurement.cpuTime
            val deltaRealMs = currentTime - previousMeasurement.timestamp
            val deltaRealSeconds = deltaRealMs / 1000.0

            if (deltaRealSeconds >= 1.0 && deltaCpuSeconds >= 0) {
                val cpuPercent = (deltaCpuSeconds / deltaRealSeconds) * 100.0
                cpuTimeCache[pid] = CpuMeasurement(segundosCpu, currentTime)
                cpuPercent.coerceIn(0.0, 100.0)
            } else {
                0.0
            }
        } else {
            cpuTimeCache[pid] = CpuMeasurement(segundosCpu, currentTime)
            0.0
        }
    }

    private fun listarUnix(): List<DataProcesos> {
        val lines = runCommand("bash", "-c", "ps -eo pid,user,pcpu,rss,stat,comm --no-headers")
        val procesos = mutableListOf<DataProcesos>()

        for (line in lines) {
            val parts = line.trim().split(Regex("\\s+"), limit = 6)
            if (parts.size < 6) continue

            val pid = parts[0].toIntOrNull() ?: continue
            val usuario = parts[1]
            val cpu = parts[2].toDoubleOrNull() ?: 0.0
            val rssKB = parts[3].toDoubleOrNull() ?: 0.0
            val memoriaMB = rssKB / 1024
            val estadoRaw = parts[4]
            val comando = parts[5]

            val estadoTraducido = traducirEstadoLinux(estadoRaw)

            procesos.add(DataProcesos(pid, comando, usuario, cpu, memoriaMB, estadoTraducido, comando))
        }
        return procesos
    }

    private fun traducirEstadoLinux(estadoRaw: String): String {
        val estadoPrincipal = estadoRaw.firstOrNull()?.uppercaseChar() ?: return "Unknown"

        return when (estadoPrincipal) {
            'R' -> "Running"
            'S' -> "Sleeping"
            'D' -> "Disk Sleep"
            'Z' -> "Zombie"
            'T' -> "Stopped"
            'I' -> "Idle"
            else -> "Unknown"
        }
    }


    fun detectarTipoProcesoWindows(pid: Int, nombre: String): String {
        if (tipoProcesoCache.containsKey(pid)) {
            return tipoProcesoCache[pid]!!
        }

        val tipo = try {
            when {
                pid < 100 || nombre.lowercase() in listOf(
                    "system", "system idle process", "registry",
                    "smss.exe", "csrss.exe", "wininit.exe",
                    "services.exe", "lsass.exe", "winlogon.exe",
                    "svchost.exe", "wudfhost.exe"
                ) -> "Sistema"

                else -> {
                    val output = runCommand("cmd", "/c", "tasklist /svc /FI \"PID eq $pid\" /FO CSV /NH")

                    if (output.isNotEmpty()) {
                        val line = output.firstOrNull() ?: ""
                        val cols = parseCsv(line)
                        val serviceName = cols.getOrNull(1)?.trim('"') ?: "N/A"

                        if (serviceName != "N/A" && serviceName.isNotBlank()) {
                            "Servicio"
                        } else {
                            "Aplicación"
                        }
                    } else {
                        "Aplicación"
                    }
                }
            }
        } catch (e: Exception) {
            "Aplicación"
        }

        tipoProcesoCache[pid] = tipo
        return tipo
    }


    fun detectarTipoProcesoLinux(pid: Int, usuario: String, comando: String): String {
        if (tipoProcesoCache.containsKey(pid)) {
            return tipoProcesoCache[pid]!!
        }

        val tipo = try {
            when {
                pid == 1 -> "Sistema"

                pid < 100 -> "Sistema"

                else -> {
                    val ppidOutput = runCommand("bash", "-c", "ps -o ppid= -p $pid 2>/dev/null")
                    val ppid = ppidOutput.firstOrNull()?.trim()?.toIntOrNull() ?: 0

                    when {
                        ppid == 1 && usuario == "root" -> "Servicio"

                        usuario == "root" && pid < 1000 -> "Sistema"

                        comando.endsWith("d") && usuario == "root" -> "Servicio"

                        else -> "Aplicación"
                    }
                }
            }
        } catch (e: Exception) {
            "Aplicación"
        }

        tipoProcesoCache[pid] = tipo
        return tipo
    }

    private fun parseCsv(line: String): List<String> {
        val out = mutableListOf<String>()
        var inQuotes = false
        var current = StringBuilder()
        for (c in line) {
            when (c) {
                '"' -> inQuotes = !inQuotes
                ',' -> if (inQuotes) current.append(c) else {
                    out.add(current.toString())
                    current = StringBuilder()
                }
                else -> current.append(c)
            }
        }
        out.add(current.toString())
        return out
    }

    suspend fun killProcess(pid: Int): KillResult = withContext(Dispatchers.IO) {
        try {
            when (os) {
                SistemaOperativo.WINDOWS -> {
                    val process = ProcessBuilder("cmd", "/c", "taskkill /PID $pid /F")
                        .redirectErrorStream(true)
                        .start()

                    val output = process.inputStream.bufferedReader().readText()
                    val exitCode = process.waitFor()

                    when {
                        exitCode == 0 -> KillResult.Success
                        output.contains("Access is denied", ignoreCase = true) ||
                                output.contains("Acceso denegado", ignoreCase = true) -> {
                            KillResult.PermissionDenied("No tienes permisos para finalizar el proceso $pid. Ejecuta como administrador.")
                        }
                        output.contains("not found", ignoreCase = true) ||
                                output.contains("no se encontró", ignoreCase = true) -> {
                            KillResult.ProcessNotFound("El proceso $pid no existe o ya fue finalizado.")
                        }
                        else -> KillResult.Error("Error al finalizar proceso $pid: $output")
                    }
                }
                SistemaOperativo.LINUX, SistemaOperativo.MAC -> {
                    val process = ProcessBuilder("bash", "-c", "kill -9 $pid 2>&1")
                        .redirectErrorStream(true)
                        .start()

                    val output = process.inputStream.bufferedReader().readText()
                    val exitCode = process.waitFor()

                    when {
                        exitCode == 0 -> KillResult.Success
                        output.contains("Operation not permitted", ignoreCase = true) ||
                                output.contains("Permission denied", ignoreCase = true) -> {
                            KillResult.PermissionDenied("No tienes permisos para finalizar el proceso $pid. Usa sudo o ejecuta como root.")
                        }
                        output.contains("No such process", ignoreCase = true) -> {
                            KillResult.ProcessNotFound("El proceso $pid no existe o ya fue finalizado.")
                        }
                        else -> KillResult.Error("Error al finalizar proceso $pid: $output")
                    }
                }
                else -> KillResult.Error("Sistema operativo no soportado")
            }
        } catch (e: SecurityException) {
            KillResult.PermissionDenied("Permisos insuficientes: ${e.message}")
        } catch (e: Exception) {
            KillResult.Error("Error inesperado: ${e.message}")
        }
    }

    sealed class KillResult {
        object Success : KillResult()
        data class PermissionDenied(val msg: String) : KillResult()
        data class ProcessNotFound(val msg: String) : KillResult()
        data class Error(val msg: String) : KillResult()
    }

    fun getMemoriaTotalPorcentaje(): Double {
        return try {
            val osBean = ManagementFactory.getOperatingSystemMXBean() as OperatingSystemMXBean
            val total = osBean.totalPhysicalMemorySize.toDouble()
            val libre = osBean.freePhysicalMemorySize.toDouble()
            ((total - libre) / total) * 100
        } catch (e: Exception) {
            0.0
        }
    }

    suspend fun getCpuTotalPorcentaje(): Double = withContext(Dispatchers.IO) {
        try {
            val osBean = ManagementFactory.getOperatingSystemMXBean() as OperatingSystemMXBean
            var cpuLoad = osBean.cpuLoad

            if (cpuLoad < 0) {
                kotlinx.coroutines.delay(100)
                cpuLoad = osBean.cpuLoad
            }

            val cpuPercent = cpuLoad * 100
            if (cpuPercent < 0) 0.0 else cpuPercent.coerceIn(0.0, 100.0)
        } catch (e: Exception) {
            0.0
        }
    }

    private fun detectarSO(): SistemaOperativo {
        val osName = System.getProperty("os.name").lowercase()
        return when {
            osName.contains("win") -> SistemaOperativo.WINDOWS
            osName.contains("linux") -> SistemaOperativo.LINUX
            osName.contains("mac") -> SistemaOperativo.MAC
            else -> SistemaOperativo.DESCONOCIDO
        }
    }

    enum class SistemaOperativo {
        WINDOWS, LINUX, MAC, DESCONOCIDO
    }
}