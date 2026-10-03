package com.tjoeun.goldenstep.global.dto.response;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Spring 및 FastAPI 준비 상태")
public record HealthResponse(

        @Schema(
                description = "전체 상태",
                example = "UP",
                allowableValues = {"UP", "DEGRADED"}
        )
        String status,

        @Schema(description = "Spring 응답 상태", example = "UP")
        String spring,

        @Schema(
                description = "FastAPI 준비 상태. "
                        + "UNAVAILABLE은 연결 실패, 시간 초과, "
                        + "초기화 중 또는 잘못된 응답을 포함",
                example = "UP",
                allowableValues = {"UP", "UNAVAILABLE"}
        )
        String fastApi,

        @Schema(description = "확인 완료 시각")
        Instant checkedAt
) {
    public boolean isReady() {
        return "UP".equals(status);
    }
}