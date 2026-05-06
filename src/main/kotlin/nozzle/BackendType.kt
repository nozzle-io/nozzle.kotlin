package nozzle

enum class BackendType(val value: Int) {
    UNKNOWN(0),
    D3D11(1),
    METAL(2),
    OPENGL(3),
    DMA_BUF(4);

    override fun toString(): String = when (this) {
        D3D11 -> "d3d11"
        METAL -> "metal"
        OPENGL -> "opengl"
        DMA_BUF -> "dma_buf"
        UNKNOWN -> "unknown"
    }

    companion object {
        fun fromValue(value: Int): BackendType =
            entries.firstOrNull { it.value == value } ?: UNKNOWN
    }
}
