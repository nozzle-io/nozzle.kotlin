package nozzle

data class ResolvedTextureFormat(
    val storageFormat: TextureFormat,
    val semanticFormat: TextureFormat,
    val formatSource: FormatSource,
    val nativeBackend: BackendType,
    val nativeKind: NativeFormatKind,
    val nativeValue: Int,
    val channelOrder: Int,
    val componentType: Int,
    val componentBits: Int,
    val channelCount: Int,
    val bytesPerPixel: Int
)
