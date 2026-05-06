package nozzle

import java.nio.ByteBuffer

class MappedPixels(
    val data: ByteBuffer,
    val rowStrideBytes: Int,
    val width: Int,
    val height: Int,
    val format: TextureFormat,
    val origin: TextureOrigin,
    private val framePtr: Long,
    private val writable: Boolean
) : AutoCloseable {

    fun row(y: Int): ByteBuffer {
        require(y >= 0 && y < height) { "row $y out of bounds (height $height)" }
        val start = y * rowStrideBytes
        val slice = data.duplicate()
        slice.position(start)
        slice.limit(start + rowStrideBytes)
        return slice.slice()
    }

    fun unmap() = close()

    override fun close() {
        if (framePtr == 0L) return
        if (writable) {
            NozzleNative.unlockWritablePixels(framePtr)
        } else {
            NozzleNative.unlockPixels(framePtr)
        }
    }
}
