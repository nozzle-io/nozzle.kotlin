package nozzle

enum class ReceiveMode(val value: Int) {
    LATEST_ONLY(0),
    SEQUENTIAL_BEST_EFFORT(1);

    companion object {
        fun fromValue(value: Int): ReceiveMode =
            entries.firstOrNull { it.value == value } ?: LATEST_ONLY
    }
}
