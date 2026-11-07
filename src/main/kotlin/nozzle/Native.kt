package nozzle

import java.nio.ByteBuffer

internal object NozzleNative {
    // Strict standard UTF-8, not JNI's modified UTF-8 or replacement encoding.
    @JvmStatic
    private fun encodeUtf8(text: String?): ByteArray {
        val value = text ?: ""
        require('\u0000' !in value) { "embedded NUL in name" }
        val bytes = Charsets.UTF_8.newEncoder()
            .onMalformedInput(java.nio.charset.CodingErrorAction.REPORT)
            .onUnmappableCharacter(java.nio.charset.CodingErrorAction.REPORT)
            .encode(java.nio.CharBuffer.wrap(value))
        return ByteArray(bytes.remaining()).also { bytes.get(it) }
    }

    @JvmStatic
    private fun decodeUtf8(bytes: ByteArray): String = Charsets.UTF_8.newDecoder()
        .onMalformedInput(java.nio.charset.CodingErrorAction.REPORT)
        .onUnmappableCharacter(java.nio.charset.CodingErrorAction.REPORT)
        .decode(ByteBuffer.wrap(bytes)).toString()

    @JvmStatic
    private fun senderInfo(name: ByteArray, app: ByteArray, id: ByteArray, backend: Int): SenderInfo =
        SenderInfo(decodeUtf8(name), decodeUtf8(app), decodeUtf8(id), BackendType.fromValue(backend))

    @JvmStatic
    private fun connectedInfo(name: ByteArray, app: ByteArray, id: ByteArray, backend: Int,
        width: Int, height: Int, format: Int, fps: Double, frame: Long, time: Long): ConnectedSenderInfo =
        ConnectedSenderInfo(decodeUtf8(name), decodeUtf8(app), decodeUtf8(id), BackendType.fromValue(backend),
            width, height, TextureFormat.fromValue(format), fps, frame, time)

    init {
        System.loadLibrary("nozzle_jni")
    }

    external fun senderCreate(
        name: String, applicationName: String,
        ringBufferSize: Int, allowFormatFallback: Boolean
    ): Long

    external fun senderDestroy(senderPtr: Long)

    external fun senderPublishTexture(senderPtr: Long, texturePtr: Long)

    external fun senderAcquireWritableFrame(
        senderPtr: Long, width: Int, height: Int, format: Int
    ): Long

    external fun senderCommitFrame(senderPtr: Long, framePtr: Long)

    external fun senderPublishGLTexture(
        senderPtr: Long, glTextureName: Int, glTarget: Int,
        width: Int, height: Int, format: Int
    )

    external fun senderPublishNativeTexture(
        senderPtr: Long, nativeTexture: Long,
        width: Int, height: Int, format: Int
    )

    external fun senderGetInfo(senderPtr: Long): SenderInfo

    external fun receiverCreate(
        name: String, applicationName: String, receiveMode: Int
    ): Long

    external fun receiverDestroy(receiverPtr: Long)

    external fun receiverAcquireFrame(receiverPtr: Long, timeoutMs: Long): Long

    external fun receiverGetConnectedInfo(receiverPtr: Long): ConnectedSenderInfo

    external fun frameRelease(framePtr: Long)

    external fun frameGetInfo(framePtr: Long): FrameInfo

    external fun frameGetResolvedFormat(framePtr: Long): ResolvedTextureFormat

    external fun frameLockPixels(framePtr: Long, origin: Int): MappedPixels

    external fun frameLockWritablePixels(framePtr: Long, origin: Int): MappedPixels

    external fun unlockPixels(framePtr: Long)

    external fun unlockWritablePixels(framePtr: Long)

    external fun frameCopyToGLTexture(
        framePtr: Long, glTextureName: Int, glTarget: Int,
        width: Int, height: Int, format: Int
    )

    external fun frameCopyToNativeTexture(
        framePtr: Long, nativeTexture: Long,
        width: Int, height: Int, format: Int
    )

    external fun deviceGetDefault(): Long

    external fun deviceDestroy(devicePtr: Long)

    external fun textureWrap(
        nativeTexture: Long, width: Int, height: Int,
        format: Int, backend: Int
    ): Long

    external fun textureDestroy(texturePtr: Long)

    external fun enumerateSenders(): Array<SenderInfo>

    external fun swizzleChannels(
        src: ByteBuffer, dst: ByteBuffer,
        width: Int, height: Int,
        srcRowBytes: Int, dstRowBytes: Int,
        format: Int, permuteMap: ByteArray
    )

    external fun widenUint16ToUint32(
        src: ByteBuffer, dst: ByteBuffer,
        width: Int, height: Int,
        srcRowBytes: Int, dstRowBytes: Int,
        channels: Int
    )

    external fun convertUint32ToFloat32(
        src: ByteBuffer, dst: ByteBuffer,
        width: Int, height: Int,
        srcRowBytes: Int, dstRowBytes: Int,
        channels: Int
    )
}
