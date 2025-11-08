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
    private fun convertirCpuTimeACpuPorcentaje(cpuTime: String): Double {
        return try {
            val partes = cpuTime.split(":").map { it.toDoubleOrNull() ?: 0.0 }
            val totalSegundos = when (partes.size) {
                3 -> partes[0] * 3600 + partes[1] * 60 + partes[2]
                2 -> partes[0] * 60 + partes[1]
                else -> partes.firstOrNull() ?: 0.0
            }

            (totalSegundos % 100).coerceAtMost(99.9)
        } catch (e: Exception) {
            0.0
        }
    }

    private fun listarWindows(): List<DataProcesos> {
        val lines = runCommand("cmd", "/c", "tasklist /v /fo csv /nh")
        val procesos = mutableListOf<DataProcesos>()

        for (line in lines) {
            val cols = parseCsv(line)
            if (cols.size < 5) continue

            val nombre = cols[0].trim('"')
            val pid = cols[1].trim('"').toIntOrNull() ?: continue
            val usuario = cols.getOrNull(6)?.trim('"')

            val memStr = cols.getOrNull(4)
                ?.trim('"')
                ?.replace("[^0-9]".toRegex(), "")
                ?: "0"

            val memoriaMB = memStr.toDoubleOrNull()?.div(1024) ?: 0.0

            val estadoRaw = cols.getOrNull(5)?.trim('"') ?: "Desconocido"
            val estado = if (estadoRaw.equals("Unknown", true)) "Desconocido" else estadoRaw
            val comando = nombre

            val cpuTimeStr = cols.getOrNull(7)?.trim('"') ?: "00:00:00"
            val cpuPercent = convertirCpuTimeACpuPorcentaje(cpuTimeStr)

            procesos.add(DataProcesos(pid, nombre, usuario, cpuPercent, memoriaMB, estado, comando))
        }
        return procesos
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
            val estado = parts[4]
            val comando = parts[5]

            procesos.add(DataProcesos(pid, comando, usuario, cpu, memoriaMB, estado, comando))
        }
        return procesos
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
                    val process = ProcessBuilder("cmd", "/c", "taskkill /PID $pid /F").start()
                    val exit = process.waitFor()
                    if (exit == 0) KillResult.Success else KillResult.Error("Error al finalizar proceso")
                }
                SistemaOperativo.LINUX, SistemaOperativo.MAC -> {
                    val process = ProcessBuilder("bash", "-c", "kill -9 $pid").start()
                    val exit = process.waitFor()
                    if (exit == 0) KillResult.Success else KillResult.Error("Error al finalizar proceso")
                }
                else -> KillResult.Error("SO no soportado")
            }
        } catch (e: Exception) {
            KillResult.Error("Error: ${e.message}")
        }
    }

    sealed class KillResult {
        object Success : KillResult()
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
