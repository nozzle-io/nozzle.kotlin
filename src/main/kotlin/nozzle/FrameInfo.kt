package nozzle

data class FrameInfo(
    val frameIndex: Long,
    val timestampNs: Long,
    val width: Int,
    val height: Int,
    val format: TextureFormat,
    val droppedFrameCount: Int
)
