package nozzle

class Device private constructor(private var ptr: Long) : AutoCloseable {

    companion object {
        fun getDefault(): Device {
            val ptr = NozzleNative.deviceGetDefault()
            return Device(ptr)
        }
    }

    override fun close() {
        if (ptr != 0L) {
            NozzleNative.deviceDestroy(ptr)
            ptr = 0L
        }
    }

    internal val handle: Long get() = ptr
}
