package nozzle

class WritableFrame internal constructor(internal val ptr: Long) : AutoCloseable {

    fun info(): FrameInfo = NozzleNative.frameGetInfo(ptr)

    fun lockWritablePixels(origin: TextureOrigin): MappedPixels =
        NozzleNative.frameLockWritablePixels(ptr, origin.value)

    fun unlockWritablePixels() {
        NozzleNative.unlockWritablePixels(ptr)
    }

    override fun close() {
        NozzleNative.frameRelease(ptr)
    }
}
