package nozzle

enum class TextureOrigin(val value: Int) {
    TOP_LEFT(0),
    BOTTOM_LEFT(1);

    companion object {
        fun fromValue(value: Int): TextureOrigin =
            entries.firstOrNull { it.value == value } ?: TOP_LEFT
    }
}
