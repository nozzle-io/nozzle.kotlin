package nozzle

import java.nio.ByteBuffer

object Nozzle {

    fun enumerateSenders(): Array<SenderInfo> = NozzleNative.enumerateSenders()

    fun isGpuAvailable(): Boolean {
        // On CI there is no GPU — avoid native crash (SIGSEGV in Metal init)
        if (System.getenv("CI") != null || System.getenv("GITHUB_ACTIONS") != null) {
            return false
        }
        val sender = try {
            Sender.create(SenderDesc("nozzle-kotlin-gpu-check", "nozzle-kotlin"))
        } catch (_: NozzleException) {
            return false
        }
        sender.close()
        return true
    }

    fun swizzleChannels(
        src: ByteBuffer, dst: ByteBuffer,
        width: Int, height: Int,
        srcRowBytes: Int, dstRowBytes: Int,
        format: TextureFormat, permuteMap: ByteArray
    ) {
        NozzleNative.swizzleChannels(src, dst, width, height, srcRowBytes, dstRowBytes, format.value, permuteMap)
    }

    fun widenUint16ToUint32(
        src: ByteBuffer, dst: ByteBuffer,
        width: Int, height: Int,
        srcRowBytes: Int, dstRowBytes: Int,
        channels: Int
    ) {
        NozzleNative.widenUint16ToUint32(src, dst, width, height, srcRowBytes, dstRowBytes, channels)
    }

    fun convertUint32ToFloat32(
        src: ByteBuffer, dst: ByteBuffer,
        width: Int, height: Int,
        srcRowBytes: Int, dstRowBytes: Int,
        channels: Int
    ) {
        NozzleNative.convertUint32ToFloat32(src, dst, width, height, srcRowBytes, dstRowBytes, channels)
    }
}
