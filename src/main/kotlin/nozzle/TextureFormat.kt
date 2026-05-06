package nozzle

enum class TextureFormat(val value: Int) {
    UNKNOWN(0),
    R8_UNORM(1),
    RG8_UNORM(2),
    RGBA8_UNORM(3),
    BGRA8_UNORM(4),
    RGBA8_SRGB(5),
    BGRA8_SRGB(6),
    R16_UNORM(7),
    RG16_UNORM(8),
    RGBA16_UNORM(9),
    R16_FLOAT(10),
    RG16_FLOAT(11),
    RGBA16_FLOAT(12),
    R32_FLOAT(13),
    RG32_FLOAT(14),
    RGBA32_FLOAT(15),
    R32_UINT(16),
    RGBA32_UINT(17),
    DEPTH32_FLOAT(18);

    fun bytesPerPixel(): Int = when (this) {
        R8_UNORM -> 1
        RG8_UNORM, R16_UNORM, R16_FLOAT -> 2
        RGBA8_UNORM, BGRA8_UNORM, RGBA8_SRGB, BGRA8_SRGB,
        RG16_UNORM, RG16_FLOAT, R32_FLOAT, R32_UINT, DEPTH32_FLOAT -> 4
        RGBA16_UNORM, RGBA16_FLOAT, RG32_FLOAT -> 8
        RGBA32_FLOAT, RGBA32_UINT -> 16
        UNKNOWN -> 0
    }

    companion object {
        fun fromValue(value: Int): TextureFormat =
            entries.firstOrNull { it.value == value } ?: UNKNOWN
    }
}
