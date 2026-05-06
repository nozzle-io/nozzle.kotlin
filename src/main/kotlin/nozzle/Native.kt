package nozzle

import java.nio.ByteBuffer

internal object NozzleNative {
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
