package nozzle

data class SenderInfo(
    val name: String,
    val applicationName: String,
    val id: String,
    val backend: BackendType
)
