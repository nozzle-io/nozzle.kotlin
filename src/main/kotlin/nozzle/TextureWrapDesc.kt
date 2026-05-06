package nozzle

data class TextureWrapDesc(
    val nativeTexture: Long,
    val width: Int,
    val height: Int,
    val format: TextureFormat,
    val backend: BackendType
)
