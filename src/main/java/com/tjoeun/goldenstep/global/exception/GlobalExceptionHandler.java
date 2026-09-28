package com.tjoeun.goldenstep.global.exception;

import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RestException.class)
    public ErrorResponse handleRestException(
            RestException exception,
            HttpServletResponse response
    ) {
        ErrorCode errorCode = exception.getErrorCode();
        response.setStatus(errorCode.getStatus());

        return ErrorResponse.of(errorCode);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ErrorResponse handleValidationException(
            MethodArgumentNotValidException exception,
            HttpServletResponse response
    ) {
        ErrorCode errorCode = ErrorCode.INVALID_INPUT;
        response.setStatus(errorCode.getStatus());

        return ErrorResponse.of(errorCode, exception.getBindingResult());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ErrorResponse handleUnreadableRequest(
            HttpMessageNotReadableException exception,
            HttpServletResponse response
    ) {
        ErrorCode errorCode = isMissingBody(exception)
                ? ErrorCode.REQUEST_BODY_MISSING
                : ErrorCode.INVALID_REQUEST_BODY;

        response.setStatus(errorCode.getStatus());
        return ErrorResponse.of(errorCode);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ErrorResponse handleMissingParameter(
            MissingServletRequestParameterException exception,
            HttpServletResponse response
    ) {
        ErrorCode errorCode = ErrorCode.MISSING_REQUEST_PARAMETER;
        response.setStatus(errorCode.getStatus());

        return ErrorResponse.of(errorCode);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ErrorResponse handleTypeMismatch(
            MethodArgumentTypeMismatchException exception,
            HttpServletResponse response
    ) {
        ErrorCode errorCode = ErrorCode.INVALID_INPUT;
        response.setStatus(errorCode.getStatus());

        return ErrorResponse.of(errorCode);
    }

    @ExceptionHandler(Exception.class)
    public ErrorResponse handleUnexpectedException(
            Exception exception,
            HttpServletResponse response
    ) {
        log.error("처리하지 못한 서버 오류가 발생했습니다.", exception);

        ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;
        response.setStatus(errorCode.getStatus());

        return ErrorResponse.of(errorCode);
    }

    private boolean isMissingBody(HttpMessageNotReadableException exception) {
        String message = exception.getMessage();
        return message != null
                && message.startsWith("Required request body is missing");
    }
}