package nozzle

class NozzleException(val errorCode: ErrorCode, message: String = errorCode.message) : RuntimeException(message) {

    constructor(value: Int) : this(ErrorCode.fromValue(value))
}
