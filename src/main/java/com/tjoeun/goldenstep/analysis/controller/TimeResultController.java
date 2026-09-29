package com.tjoeun.goldenstep.analysis.controller;

import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tjoeun.goldenstep.analysis.dto.response.TimeResultResponse;
import com.tjoeun.goldenstep.analysis.service.TimeResultService;
import com.tjoeun.goldenstep.global.exception.ErrorResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "시간점 결과", description = "저장된 시간점별 탐색 결과 API")
@RestController
@RequestMapping("/api/search/analysis")
@RequiredArgsConstructor
public class TimeResultController {

    private final TimeResultService timeResultService;

    @Operation(
            summary = "저장된 시간점 결과 조회",
            description = """
                    복구 쿠키에 연결된 분석 실행에서 해당 시간점의 저장된 결과를 조회합니다.
                    결과가 아직 없으면 404를 반환하며, 현재는 FastAPI를 호출하지 않습니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "저장된 결과 조회 성공",
                    content = @Content(
							schema = @Schema(implementation = TimeResultResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "지원하지 않는 시간점",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "복구 쿠키가 없거나 유효한 세션을 찾을 수 없음",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "분석 실행 또는 저장된 시간점 결과를 찾을 수 없음",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "410",
                    description = "탐색 세션 보관 기간 만료",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    @GetMapping("/{runId}/results/{timePoint}")
    public TimeResultResponse getStoredResult(
            @PathVariable Long runId,
            @PathVariable String timePoint,
			@CookieValue(name = "GOLDENSTEP_RECOVERY", required = false) String recoveryToken
    ) {
		return timeResultService.getStoredResult(runId, timePoint, recoveryToken);
    }
}