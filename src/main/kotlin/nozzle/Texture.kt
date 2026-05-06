package nozzle

class Texture private constructor(private var ptr: Long) : AutoCloseable {

    companion object {
        fun wrap(desc: TextureWrapDesc): Texture {
            val ptr = NozzleNative.textureWrap(
                desc.nativeTexture, desc.width, desc.height,
                desc.format.value, desc.backend.value
            )
            return Texture(ptr)
        }
    }

    override fun close() {
        if (ptr != 0L) {
            NozzleNative.textureDestroy(ptr)
            ptr = 0L
        }
    }

    internal val handle: Long get() = ptr
}
