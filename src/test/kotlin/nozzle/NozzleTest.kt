package nozzle

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledOnOs
import org.junit.jupiter.api.condition.OS
import java.nio.ByteBuffer

class NozzleTest {

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

    @Test
    fun errorCodeCount() {
        assertEquals(12, ErrorCode.entries.size)
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

    @Test
    fun backendTypeCount() {
        assertEquals(5, BackendType.entries.size)
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

    @Test
    fun textureFormatCount() {
        assertEquals(19, TextureFormat.entries.size)
    }

    // ========== ReceiveMode ==========

    @Test
    fun receiveModeValues() {
        assertEquals(0, ReceiveMode.LATEST_ONLY.value)
        assertEquals(1, ReceiveMode.SEQUENTIAL_BEST_EFFORT.value)
    }

    // ========== FrameStatus ==========

    @Test
    fun frameStatusCount() {
        assertEquals(5, FrameStatus.entries.size)
    }

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

    // ========== FormatSource ==========

    @Test
    fun formatSourceCount() {
        assertEquals(4, FormatSource.entries.size)
    }

    // ========== NativeFormatKind ==========

    @Test
    fun nativeFormatKindCount() {
        assertEquals(5, NativeFormatKind.entries.size)
    }

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

    // ========== GPU tests (skipped on CI without GPU) ==========

    @Test
    @EnabledOnOs(OS.MAC)
    fun gpuAvailableOnMac() {
        assumeTrue(Nozzle.isGpuAvailable(), "no GPU available")
    }
}
