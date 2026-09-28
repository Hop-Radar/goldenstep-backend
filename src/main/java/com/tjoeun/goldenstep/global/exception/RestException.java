package com.tjoeun.goldenstep.global.exception;

import lombok.Builder;
import lombok.Getter;

@Getter
public class RestException extends RuntimeException {

	private static final long serialVersionUID = 1L;
    private final ErrorCode errorCode;

    @Builder
    public RestException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
    
    @Builder
    public RestException(String message, ErrorCode errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
}