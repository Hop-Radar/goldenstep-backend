package com.tjoeun.goldenstep.global.controller;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tjoeun.goldenstep.global.dto.response.HealthResponse;
import com.tjoeun.goldenstep.global.service.HealthService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
@Tag(name = "Health", description = "서버 준비 상태 확인")
public class HealthController {

    private final HealthService healthService;

    @GetMapping
    @Operation(
            summary = "Spring 및 FastAPI 상태 확인",
            description = "FastAPI /health의 HTTP 상태와 "
                    + "본문 status=UP을 확인합니다. "
                    + "DB 상태 및 분석 처리 여유는 검사하지 않습니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Spring 및 FastAPI 준비 완료",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = HealthResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "503",
                    description = "FastAPI 준비 상태 확인 실패",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = HealthResponse.class
                            )
                    )
            )
    })
    public ResponseEntity<HealthResponse> health() {
        HealthResponse response = healthService.check();

        // 추가: 오래된 상태 응답의 캐시 사용 방지
        return ResponseEntity
                .status(response.isReady() ? 200 : 503)
                .cacheControl(CacheControl.noStore())
                .body(response);
    }
}