package nozzle

enum class TextureFormat(val value: Int) {
    UNKNOWN(0),
    R8_UNORM(1),
    RG8_UNORM(2),
    RGB8_UNORM(3),
    RGBA8_UNORM(4),
    BGRA8_UNORM(5),
    RGBA8_SRGB(6),
    BGRA8_SRGB(7),
    R16_UNORM(8),
    RG16_UNORM(9),
    RGB16_UNORM(10),
    RGBA16_UNORM(11),
    R16_FLOAT(12),
    RG16_FLOAT(13),
    RGB16_FLOAT(14),
    RGBA16_FLOAT(15),
    R32_FLOAT(16),
    RG32_FLOAT(17),
    RGB32_FLOAT(18),
    RGBA32_FLOAT(19),
    R32_UINT(20),
    RGBA32_UINT(21),
    RGB32_UINT(22),
    DEPTH32_FLOAT(23);

    fun bytesPerPixel(): Int = when (this) {
        R8_UNORM -> 1
        RG8_UNORM, R16_UNORM, R16_FLOAT -> 2
        RGB8_UNORM -> 3
        RGBA8_UNORM, BGRA8_UNORM, RGBA8_SRGB, BGRA8_SRGB,
        RG16_UNORM, RG16_FLOAT, R32_FLOAT, R32_UINT, DEPTH32_FLOAT -> 4
        RGB16_UNORM, RGB16_FLOAT, RGBA16_UNORM, RGBA16_FLOAT, RG32_FLOAT -> 8
        RGB32_FLOAT, RGB32_UINT, RGBA32_FLOAT, RGBA32_UINT -> 16
        UNKNOWN -> 0
    }

    companion object {
        fun fromValue(value: Int): TextureFormat =
            entries.firstOrNull { it.value == value } ?: UNKNOWN
    }
}
