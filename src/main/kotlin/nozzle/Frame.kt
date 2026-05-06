package nozzle

import java.nio.ByteBuffer

class Frame internal constructor(internal val ptr: Long) : AutoCloseable {

    override fun close() {
        NozzleNative.frameRelease(ptr)
    }

    fun info(): FrameInfo = NozzleNative.frameGetInfo(ptr)

    fun resolvedFormat(): ResolvedTextureFormat = NozzleNative.frameGetResolvedFormat(ptr)

    fun lockPixels(origin: TextureOrigin): MappedPixels =
        NozzleNative.frameLockPixels(ptr, origin.value)

    fun lockWritablePixels(origin: TextureOrigin): MappedPixels =
        NozzleNative.frameLockWritablePixels(ptr, origin.value)

    fun copyToGLTexture(glTextureName: Int, glTarget: Int, width: Int, height: Int, format: TextureFormat) {
        NozzleNative.frameCopyToGLTexture(ptr, glTextureName, glTarget, width, height, format.value)
    }

    fun copyToNativeTexture(nativeTexture: Long, width: Int, height: Int, format: TextureFormat) {
        NozzleNative.frameCopyToNativeTexture(ptr, nativeTexture, width, height, format.value)
    }
}
