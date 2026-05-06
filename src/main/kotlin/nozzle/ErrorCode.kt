package nozzle

enum class ErrorCode(val value: Int) {
    OK(0),
    UNKNOWN(1),
    INVALID_ARGUMENT(2),
    UNSUPPORTED_BACKEND(3),
    UNSUPPORTED_FORMAT(4),
    DEVICE_MISMATCH(5),
    RESOURCE_CREATION_FAILED(6),
    SHARED_HANDLE_FAILED(7),
    SENDER_NOT_FOUND(8),
    SENDER_CLOSED(9),
    TIMEOUT(10),
    BACKEND_ERROR(11);

    val message: String
        get() = when (this) {
            OK -> "ok"
            UNKNOWN -> "unknown error"
            INVALID_ARGUMENT -> "invalid argument"
            UNSUPPORTED_BACKEND -> "unsupported backend"
            UNSUPPORTED_FORMAT -> "unsupported format"
            DEVICE_MISMATCH -> "device mismatch"
            RESOURCE_CREATION_FAILED -> "resource creation failed"
            SHARED_HANDLE_FAILED -> "shared handle failed"
            SENDER_NOT_FOUND -> "sender not found"
            SENDER_CLOSED -> "sender closed"
            TIMEOUT -> "timeout"
            BACKEND_ERROR -> "backend error"
        }

    companion object {
        fun fromValue(value: Int): ErrorCode =
            entries.firstOrNull { it.value == value } ?: UNKNOWN
    }
}
