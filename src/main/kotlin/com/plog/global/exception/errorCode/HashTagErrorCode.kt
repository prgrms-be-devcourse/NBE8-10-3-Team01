package com.plog.global.exception.errorCode

import org.springframework.http.HttpStatus

enum class HashTagErrorCode(
    override val httpStatus: HttpStatus,
    override val message: String
) : ErrorCode {
    HASHTAG_POST_NOT_FOUND(HttpStatus.NOT_FOUND, "해당하는 게시글을 찾을 수 없습니다."),
    HASHTAG_CREATE_FAIL(HttpStatus.BAD_REQUEST, "failed to create hashtag");
}
