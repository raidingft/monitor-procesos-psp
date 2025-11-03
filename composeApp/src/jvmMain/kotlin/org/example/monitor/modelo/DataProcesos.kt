package org.example.monitor.modelo

data class DataProcesos(
    val pid: Int,
    val nombre: String,
    val usuario: String?,
    val cpu: Double?,
    val memoria: Double?,
    val estado: String?,
    val comando: String?
)
