package nozzle

/** Owned last-observed connection state and sender information, not a fresh liveness check. */
data class ConnectionSnapshot(
    val connected: Boolean,
    val info: ConnectedSenderInfo
)
