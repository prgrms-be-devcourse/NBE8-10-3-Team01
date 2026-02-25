package com.plog.global.exception.exceptions

import com.plog.global.exception.errorCode.HashTagErrorCode

class HashTagException : BaseException {
    constructor(errorCode: HashTagErrorCode) : super(errorCode)
    constructor(errorCode: HashTagErrorCode, logMessage: String) : super(errorCode, logMessage)
    constructor(errorCode: HashTagErrorCode, logMessage: String, clientMessage: String) : super(errorCode, logMessage, clientMessage)
}
