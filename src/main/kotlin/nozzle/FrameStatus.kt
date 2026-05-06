package nozzle

enum class FrameStatus(val value: Int) {
    NEW(0),
    NO_NEW(1),
    DROPPED(2),
    SENDER_CLOSED(3),
    ERROR(4);

    companion object {
        fun fromValue(value: Int): FrameStatus =
            entries.firstOrNull { it.value == value } ?: ERROR
    }
}
