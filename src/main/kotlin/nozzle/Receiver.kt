package nozzle

class Receiver private constructor(private var ptr: Long) : AutoCloseable {

    companion object {
        fun create(desc: ReceiverDesc): Receiver {
            val ptr = NozzleNative.receiverCreate(
                desc.name, desc.applicationName, desc.receiveMode.value
            )
            return Receiver(ptr)
        }
    }

    override fun close() {
        if (ptr != 0L) {
            NozzleNative.receiverDestroy(ptr)
            ptr = 0L
        }
    }

    fun acquireFrame(timeoutMs: Long): Frame {
        val framePtr = NozzleNative.receiverAcquireFrame(ptr, timeoutMs)
        return Frame(framePtr)
    }

    fun connectionSnapshot(): ConnectionSnapshot = NozzleNative.receiverGetConnectionSnapshot(ptr)

    fun connectedInfo(): ConnectedSenderInfo = connectionSnapshot().info

    val isConnected: Boolean
        get() = try {
            connectionSnapshot().connected
        } catch (_: NozzleException) {
            false
        }

    internal val handle: Long get() = ptr
}
