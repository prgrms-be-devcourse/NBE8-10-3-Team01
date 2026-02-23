package com.plog.global.exception.errorCode;

import org.springframework.http.HttpStatus;

public enum HashTagErrorCode implements ErrorCode {
    HASHTAG_POST_NOT_FOUND(HttpStatus.NOT_FOUND, "해당하는 게시글을 찾을 수 없습니다."),
    HASHTAG_CREATE_FAIL(HttpStatus.BAD_REQUEST, "failed to create hashtag");



    private final HttpStatus status;
    private final String message;

    HashTagErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    @Override
    public HttpStatus getHttpStatus() {
        return status;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
