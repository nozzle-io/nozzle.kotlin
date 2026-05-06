package nozzle

class Sender private constructor(private var ptr: Long) : AutoCloseable {

    companion object {
        fun create(desc: SenderDesc): Sender {
            val ptr = NozzleNative.senderCreate(
                desc.name, desc.applicationName,
                desc.ringBufferSize, desc.allowFormatFallback
            )
            return Sender(ptr)
        }
    }

    override fun close() {
        if (ptr != 0L) {
            NozzleNative.senderDestroy(ptr)
            ptr = 0L
        }
    }

    fun acquireWritableFrame(width: Int, height: Int, format: TextureFormat): Frame {
        val framePtr = NozzleNative.senderAcquireWritableFrame(ptr, width, height, format.value)
        return Frame(framePtr)
    }

    fun commitFrame(frame: Frame) {
        NozzleNative.senderCommitFrame(ptr, frame.ptr)
    }

    fun publishTexture(texture: Texture) {
        NozzleNative.senderPublishTexture(ptr, texture.ptr)
    }

    fun publishGLTexture(glTextureName: Int, glTarget: Int, width: Int, height: Int, format: TextureFormat) {
        NozzleNative.senderPublishGLTexture(ptr, glTextureName, glTarget, width, height, format.value)
    }

    fun publishNativeTexture(nativeTexture: Long, width: Int, height: Int, format: TextureFormat) {
        NozzleNative.senderPublishNativeTexture(ptr, nativeTexture, width, height, format.value)
    }

    fun info(): SenderInfo = NozzleNative.senderGetInfo(ptr)

    internal val handle: Long get() = ptr
}
