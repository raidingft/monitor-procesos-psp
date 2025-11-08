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
            val estadoRaw = cols.getOrNull(5)?.trim('"') ?: "Desconocido"
            val estado = if (estadoRaw.equals("Unknown", true)) "Desconocido" else estadoRaw
            val comando = nombre

            val cpuTimeStr = cols.getOrNull(7)?.trim('"') ?: "00:00:00"
            val partes = cpuTimeStr.split(":").map { it.toDoubleOrNull() ?: 0.0 }
            val segundosCpu = when (partes.size) {
                3 -> partes[0] * 3600 + partes[1] * 60 + partes[2]
                2 -> partes[0] * 60 + partes[1]
                else -> 0.0
            }

            val cpuPercent = calcularCpuPorcentaje(pid, segundosCpu, currentTime)

            procesos.add(DataProcesos(pid, nombre, usuario, cpuPercent, memoriaMB, estado, comando))
        }

        val pidsActuales = procesos.map { it.pid }.toSet()
        cpuTimeCache.keys.retainAll(pidsActuales)

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
            val estado = parts[4]
            val comando = parts[5]

            procesos.add(DataProcesos(pid, comando, usuario, cpu, memoriaMB, estado, comando))
        }
        return procesos
    }


    private fun parseCpuTime(cpuTimeStr: String): Double {
        return try {
            val mainPart: String
            val millisPart: Double

            if (cpuTimeStr.contains('.')) {
                val parts = cpuTimeStr.split('.')
                mainPart = parts[0]
                millisPart = ("0." + parts.getOrNull(1)?.take(3)).toDoubleOrNull() ?: 0.0
            } else {
                mainPart = cpuTimeStr
                millisPart = 0.0
            }

            val timeParts = mainPart.split(":").map { it.toDoubleOrNull() ?: 0.0 }
            val totalSeconds = when (timeParts.size) {
                3 -> timeParts[0] * 3600 + timeParts[1] * 60 + timeParts[2]
                2 -> timeParts[0] * 60 + timeParts[1]
                1 -> timeParts[0]
                else -> 0.0
            }

            totalSeconds + millisPart
        } catch (e: Exception) {
            0.0
        }
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