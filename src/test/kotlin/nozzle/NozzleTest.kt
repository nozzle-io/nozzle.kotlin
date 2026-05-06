package nozzle

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledOnOs
import org.junit.jupiter.api.condition.OS
import java.nio.ByteBuffer
import java.nio.ByteOrder

class NozzleTest {

    companion object {
        private const val TEST_WIDTH = 64
        private const val TEST_HEIGHT = 64
        private const val DEFAULT_TIMEOUT_MS = 5000L
    }

    // ========== ErrorCode ==========

    @Test
    fun errorCodeOkIsZero() {
        assertEquals(0, ErrorCode.OK.value)
    }

    @Test
    fun errorCodeAllValuesUnique() {
        val values = ErrorCode.entries.map { it.value }
        assertEquals(values.size, values.toSet().size)
    }

    @Test
    fun errorCodeFromValueRoundTrip() {
        for (code in ErrorCode.entries) {
            assertEquals(code, ErrorCode.fromValue(code.value))
        }
    }

    @Test
    fun errorCodeFromValueUnknownForInvalid() {
        assertEquals(ErrorCode.UNKNOWN, ErrorCode.fromValue(999))
    }

    // ========== BackendType ==========

    @Test
    fun backendTypeUnknownIsZero() {
        assertEquals(0, BackendType.UNKNOWN.value)
    }

    @Test
    fun backendTypeFromValueRoundTrip() {
        for (bt in BackendType.entries) {
            assertEquals(bt, BackendType.fromValue(bt.value))
        }
    }

    @Test
    fun backendTypeToString() {
        assertEquals("d3d11", BackendType.D3D11.toString())
        assertEquals("metal", BackendType.METAL.toString())
        assertEquals("opengl", BackendType.OPENGL.toString())
        assertEquals("dma_buf", BackendType.DMA_BUF.toString())
        assertEquals("unknown", BackendType.UNKNOWN.toString())
    }

    // ========== TextureFormat ==========

    @Test
    fun textureFormatUnknownIsZero() {
        assertEquals(0, TextureFormat.UNKNOWN.value)
    }

    @Test
    fun textureFormatAllValuesUnique() {
        val values = TextureFormat.entries.map { it.value }
        assertEquals(values.size, values.toSet().size)
    }

    @Test
    fun textureFormatFromValueRoundTrip() {
        for (f in TextureFormat.entries) {
            assertEquals(f, TextureFormat.fromValue(f.value))
        }
    }

    @Test
    fun textureFormatBytesPerPixel() {
        assertEquals(1, TextureFormat.R8_UNORM.bytesPerPixel())
        assertEquals(2, TextureFormat.RG8_UNORM.bytesPerPixel())
        assertEquals(2, TextureFormat.R16_UNORM.bytesPerPixel())
        assertEquals(2, TextureFormat.R16_FLOAT.bytesPerPixel())
        assertEquals(4, TextureFormat.RGBA8_UNORM.bytesPerPixel())
        assertEquals(4, TextureFormat.BGRA8_UNORM.bytesPerPixel())
        assertEquals(4, TextureFormat.RGBA8_SRGB.bytesPerPixel())
        assertEquals(4, TextureFormat.R32_FLOAT.bytesPerPixel())
        assertEquals(4, TextureFormat.R32_UINT.bytesPerPixel())
        assertEquals(4, TextureFormat.DEPTH32_FLOAT.bytesPerPixel())
        assertEquals(8, TextureFormat.RGBA16_UNORM.bytesPerPixel())
        assertEquals(8, TextureFormat.RGBA16_FLOAT.bytesPerPixel())
        assertEquals(8, TextureFormat.RG32_FLOAT.bytesPerPixel())
        assertEquals(16, TextureFormat.RGBA32_FLOAT.bytesPerPixel())
        assertEquals(16, TextureFormat.RGBA32_UINT.bytesPerPixel())
        assertEquals(0, TextureFormat.UNKNOWN.bytesPerPixel())
    }

    // ========== ReceiveMode ==========

    @Test
    fun receiveModeValues() {
        assertEquals(0, ReceiveMode.LATEST_ONLY.value)
        assertEquals(1, ReceiveMode.SEQUENTIAL_BEST_EFFORT.value)
    }

    // ========== FrameStatus ==========

    @Test
    fun frameStatusValues() {
        assertEquals(0, FrameStatus.NEW.value)
        assertEquals(1, FrameStatus.NO_NEW.value)
        assertEquals(2, FrameStatus.DROPPED.value)
        assertEquals(3, FrameStatus.SENDER_CLOSED.value)
        assertEquals(4, FrameStatus.ERROR.value)
    }

    // ========== TextureOrigin ==========

    @Test
    fun textureOriginValues() {
        assertEquals(0, TextureOrigin.TOP_LEFT.value)
        assertEquals(1, TextureOrigin.BOTTOM_LEFT.value)
    }

    // ========== FormatSource / NativeFormatKind ==========

    // ========== NozzleException ==========

    @Test
    fun nozzleExceptionHasErrorCode() {
        val ex = NozzleException(ErrorCode.TIMEOUT)
        assertEquals(ErrorCode.TIMEOUT, ex.errorCode)
    }

    @Test
    fun nozzleExceptionMessageFromCode() {
        val ex = NozzleException(ErrorCode.SENDER_NOT_FOUND)
        assertEquals("sender not found", ex.message)
    }

    @Test
    fun nozzleExceptionFromInt() {
        val ex = NozzleException(10)
        assertEquals(ErrorCode.TIMEOUT, ex.errorCode)
    }

    @Test
    fun nozzleExceptionIsRuntimeException() {
        assertInstanceOf(RuntimeException::class.java, NozzleException(ErrorCode.OK))
    }

    // ========== MappedPixels bounds ==========

    @Test
    fun mappedPixelsRowNegativeThrows() {
        val buf = ByteBuffer.allocateDirect(100)
        val mp = MappedPixels(buf, 10, 10, 10, TextureFormat.R8_UNORM, TextureOrigin.TOP_LEFT, 0L, false)
        assertThrows(IllegalArgumentException::class.java) { mp.row(-1) }
    }

    @Test
    fun mappedPixelsRowAtHeightThrows() {
        val buf = ByteBuffer.allocateDirect(100)
        val mp = MappedPixels(buf, 10, 10, 10, TextureFormat.R8_UNORM, TextureOrigin.TOP_LEFT, 0L, false)
        assertThrows(IllegalArgumentException::class.java) { mp.row(10) }
    }

    @Test
    fun mappedPixelsValidRowSucceeds() {
        val buf = ByteBuffer.allocateDirect(100)
        val mp = MappedPixels(buf, 10, 10, 10, TextureFormat.R8_UNORM, TextureOrigin.TOP_LEFT, 0L, false)
        val row = mp.row(5)
        assertEquals(10, row.capacity())
    }

    // ========== CPU-only function tests ==========

    @Test
    fun swizzleChannelsRgbaToBgra() {
        val src = ByteBuffer.allocateDirect(4)
        val dst = ByteBuffer.allocateDirect(4)
        src.put(byteArrayOf(0xFF.toByte(), 0x00, 0x00, 0xFF.toByte()))
        src.rewind()
        val permuteMap = byteArrayOf(2, 1, 0, 3)
        Nozzle.swizzleChannels(src, dst, 1, 1, 4, 4, TextureFormat.RGBA8_UNORM, permuteMap)
        dst.rewind()
        assertEquals(0x00.toByte(), dst.get())
        assertEquals(0x00.toByte(), dst.get())
        assertEquals(0xFF.toByte(), dst.get())
        assertEquals(0xFF.toByte(), dst.get())
    }

    @Test
    fun swizzleChannelsMultiPixel() {
        val pixelCount = TEST_WIDTH * TEST_HEIGHT
        val bpp = 4
        val src = ByteBuffer.allocateDirect(pixelCount * bpp)
        val dst = ByteBuffer.allocateDirect(pixelCount * bpp)
        for (i in 0 until pixelCount) {
            src.put((i % 256).toByte())
            src.put(((i + 1) % 256).toByte())
            src.put(((i + 2) % 256).toByte())
            src.put(((i + 3) % 256).toByte())
        }
        src.flip()
        val permuteMap = byteArrayOf(2, 1, 0, 3)
        Nozzle.swizzleChannels(src, dst, TEST_WIDTH, TEST_HEIGHT, TEST_WIDTH * bpp, TEST_WIDTH * bpp, TextureFormat.RGBA8_UNORM, permuteMap)
        dst.rewind()
        for (i in 0 until pixelCount) {
            assertEquals(((i + 2) % 256).toByte(), dst.get())
            assertEquals(((i + 1) % 256).toByte(), dst.get())
            assertEquals((i % 256).toByte(), dst.get())
            assertEquals(((i + 3) % 256).toByte(), dst.get())
        }
    }

    @Test
    fun widenUint16ToUint32Basic() {
        val src = ByteBuffer.allocateDirect(2).order(ByteOrder.nativeOrder())
        val dst = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder())
        src.putShort(0x1234.toShort())
        src.rewind()
        Nozzle.widenUint16ToUint32(src, dst, 1, 1, 2, 4, 1)
        dst.rewind()
        assertEquals(0x1234, dst.int)
    }

    @Test
    fun widenUint16ToUint32MaxValue() {
        val src = ByteBuffer.allocateDirect(2).order(ByteOrder.nativeOrder())
        val dst = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder())
        src.putShort((-1).toShort())
        src.rewind()
        Nozzle.widenUint16ToUint32(src, dst, 1, 1, 2, 4, 1)
        dst.rewind()
        assertEquals(0xFFFF, dst.int)
    }

    @Test
    fun convertUint32ToFloat32Basic() {
        val src = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder())
        val dst = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder())
        src.putInt(42)
        src.rewind()
        Nozzle.convertUint32ToFloat32(src, dst, 1, 1, 4, 4, 1)
        dst.rewind()
        assertEquals(42.0f, dst.float, 0.001f)
    }

    @Test
    fun convertUint32ToFloat32ZeroAndMax() {
        val src = ByteBuffer.allocateDirect(8).order(ByteOrder.nativeOrder())
        val dst = ByteBuffer.allocateDirect(8).order(ByteOrder.nativeOrder())
        src.putInt(0)
        src.putInt(Int.MAX_VALUE)
        src.rewind()
        Nozzle.convertUint32ToFloat32(src, dst, 2, 1, 8, 8, 1)
        dst.rewind()
        assertEquals(0.0f, dst.float, 0.001f)
        assertEquals(Int.MAX_VALUE.toFloat(), dst.float, 0.001f)
    }

    // ========== Error path tests ==========

    @Test
    fun textureFormatUnknownBytesPerPixel() {
        assertEquals(0, TextureFormat.UNKNOWN.bytesPerPixel())
    }

    // ========== Discovery ==========

    @Test
    fun enumerateSendersReturnsArray() {
        val senders = Nozzle.enumerateSenders()
        assertNotNull(senders)
        for (s in senders) {
            assertNotNull(s.name)
        }
    }

    // ========== GPU tests (skipped on CI without GPU) ==========

    @Test
    @EnabledOnOs(OS.MAC)
    fun gpuAvailableOnMac() {
        assumeTrue(Nozzle.isGpuAvailable(), "no GPU available")
    }
}
