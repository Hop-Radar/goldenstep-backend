package com.tjoeun.goldenstep.global.exception;

import lombok.Getter;

@Getter
public class RestException extends RuntimeException {

	private static final long serialVersionUID = 1L;
    private final ErrorCode errorCode;

    public RestException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}