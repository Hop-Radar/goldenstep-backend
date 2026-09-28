package com.tjoeun.goldenstep.global.exception;

import java.util.List;

import org.springframework.validation.BindingResult;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "API 오류 응답")
public record ErrorResponse(
        boolean success,
        int status,
        String code,
        String message,
        List<FieldErrorDetail> errors
) {

    public static ErrorResponse of(ErrorCode errorCode) {
        return new ErrorResponse(
                false,
                errorCode.getStatus(),
                errorCode.getDivisionCode(),
                errorCode.getMessage(),
                List.of()
        );
    }

    public static ErrorResponse of(
            ErrorCode errorCode,
            BindingResult bindingResult
    ) {
        List<FieldErrorDetail> errors = bindingResult.getFieldErrors()
                .stream()
                .map(error -> new FieldErrorDetail(
                        error.getField(),
                        error.getDefaultMessage() == null
                                ? "올바른 값을 입력해주세요."
                                : error.getDefaultMessage()
                ))
                .toList();

        return new ErrorResponse(
                false,
                errorCode.getStatus(),
                errorCode.getDivisionCode(),
                errorCode.getMessage(),
                errors
        );
    }

    public record FieldErrorDetail(
            String field,
            String reason
    ) {
    }
}