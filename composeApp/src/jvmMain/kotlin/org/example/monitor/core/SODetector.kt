package org.example.monitor.core

enum class SO {
    WINDOWS, LINUX, MAC, UNKNOWN
}

object SODetector {
    fun detect(): SO {
        val soName = System.getProperty("os.name").lowercase()
        return when {
            soName.contains("win") -> SO.WINDOWS
            soName.contains("linux") -> SO.LINUX
            soName.contains("mac") -> SO.MAC
            else -> SO.UNKNOWN
        }
    }
}
