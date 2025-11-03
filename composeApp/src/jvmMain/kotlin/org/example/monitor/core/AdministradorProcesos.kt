package org.example.monitor.core

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.example.monitor.modelo.DataProcesos
import java.io.BufferedReader
import java.io.InputStreamReader

class AdministradorProcesos {

    private val so = SODetector.detect()

    suspend fun listProcesses(): List<DataProcesos> = withContext(Dispatchers.IO) {
        try {
            when (so) {
                SO.WINDOWS -> listWindows()
                SO.LINUX, SO.MAC -> listUnix()
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

    private fun listWindows(): List<DataProcesos> {
        val lines = runCommand("cmd", "/c", "tasklist /v /fo csv")
        if (lines.isEmpty()) return emptyList()

        val result = mutableListOf<DataProcesos>()
        for (i in 1 until lines.size) {
            val cols = parseCsv(lines[i])
            if (cols.size < 2) continue

            val name = cols.getOrNull(0)?.trim('"') ?: continue
            val pid = cols.getOrNull(1)?.trim('"')?.toIntOrNull() ?: continue
            val user = cols.getOrNull(6)?.trim('"')
            val mem = cols.getOrNull(4)?.replace("[^0-9]".toRegex(), "")?.toDoubleOrNull()

            val cpu = null
            val estado = "Running"
            val comando = name

            result.add(DataProcesos(pid, name, user, cpu, mem, estado, comando))
        }
        return result
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

    private fun listUnix(): List<DataProcesos> {
        val lines = runCommand("bash", "-c", "ps -eo pid,user,pcpu,pmem,stat,comm --no-headers")
        val result = mutableListOf<DataProcesos>()
        for (line in lines) {
            val parts = line.trim().split(Regex("\\s+"), limit = 6)
            if (parts.size < 6) continue
            val pid = parts[0].toIntOrNull() ?: continue
            val user = parts[1]
            val cpu = parts[2].toDoubleOrNull()
            val mem = parts[3].toDoubleOrNull()
            val estado = when {
                parts[4].startsWith("R") -> "Running"
                parts[4].startsWith("S") -> "Sleeping"
                parts[4].startsWith("Z") -> "Zombie"
                else -> "Other"
            }
            val comando = parts[5]
            result.add(DataProcesos(pid, comando, user, cpu, mem, estado, comando))
        }
        return result
    }


    suspend fun killProcess(pid: Int): KillResult = withContext(Dispatchers.IO) {
        try {
            when (so) {
                SO.WINDOWS -> {
                    val process = ProcessBuilder("cmd", "/c", "taskkill /PID $pid /F").start()
                    val exit = process.waitFor()
                    if (exit == 0) KillResult.Success else KillResult.Error("Error al finalizar proceso")
                }
                SO.LINUX, SO.MAC -> {
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
}
