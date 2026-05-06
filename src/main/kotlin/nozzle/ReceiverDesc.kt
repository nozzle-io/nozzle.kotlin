package nozzle

data class ReceiverDesc(
    val name: String,
    val applicationName: String,
    val receiveMode: ReceiveMode = ReceiveMode.LATEST_ONLY
)
