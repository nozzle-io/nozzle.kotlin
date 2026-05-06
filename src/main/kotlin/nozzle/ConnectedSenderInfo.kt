package nozzle

data class ConnectedSenderInfo(
    val name: String,
    val applicationName: String,
    val id: String,
    val backend: BackendType,
    val width: Int,
    val height: Int,
    val format: TextureFormat,
    val estimatedFps: Double,
    val frameCounter: Long,
    val lastUpdateTimeNs: Long
)
