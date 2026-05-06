package nozzle

class NozzleException(val errorCode: ErrorCode) : RuntimeException(errorCode.message) {
    constructor(errorCode: ErrorCode, message: String) : super(message)

    constructor(value: Int) : this(ErrorCode.fromValue(value))
}
