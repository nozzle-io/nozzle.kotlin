package nozzle

enum class FormatSource(val value: Int) {
    UNKNOWN(0),
    REQUESTED(1),
    CALLER_HINT(2),
    NATIVE_OBSERVED(3);

    companion object {
        fun fromValue(value: Int): FormatSource =
            entries.firstOrNull { it.value == value } ?: UNKNOWN
    }
}
