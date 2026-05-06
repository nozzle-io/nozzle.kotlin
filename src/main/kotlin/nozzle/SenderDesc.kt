package nozzle

data class SenderDesc(
    val name: String,
    val applicationName: String,
    val ringBufferSize: Int = 3,
    val allowFormatFallback: Boolean = false
)
