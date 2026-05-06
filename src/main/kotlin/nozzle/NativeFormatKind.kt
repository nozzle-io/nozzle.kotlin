package nozzle

enum class NativeFormatKind(val value: Int) {
    UNKNOWN(0),
    MTL_PIXEL_FORMAT(1),
    DXGI_FORMAT(2),
    DRM_FOURCC(3),
    GL_INTERNAL_FORMAT(4);

    companion object {
        fun fromValue(value: Int): NativeFormatKind =
            entries.firstOrNull { it.value == value } ?: UNKNOWN
    }
}
