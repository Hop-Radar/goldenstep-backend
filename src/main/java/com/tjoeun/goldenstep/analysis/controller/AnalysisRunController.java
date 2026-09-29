package com.tjoeun.goldenstep.analysis.controller;

import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tjoeun.goldenstep.analysis.dto.response.AnalysisStatusResponse;
import com.tjoeun.goldenstep.analysis.service.AnalysisRunService;
import com.tjoeun.goldenstep.global.exception.ErrorResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "분석", description = "탐색 분석 API")
@RestController
@RequestMapping("/api/search/analysis")
@RequiredArgsConstructor
public class AnalysisRunController {

    private final AnalysisRunService analysisRunService;

    @Operation(
            summary = "분석 진행 상태 조회",
            description = """
                    복구 쿠키로 탐색 세션을 확인한 뒤 해당 세션의 분석 상태를 반환합니다.
                    이 요청은 새 분석을 생성하지 않습니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "분석 상태 조회 성공",
                    content = @Content(
							schema = @Schema(implementation = AnalysisStatusResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "복구 쿠키가 없거나 유효한 탐색 세션을 찾을 수 없음",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "분석 실행이 없거나 이 탐색 세션에 속하지 않음",
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
    @GetMapping("/{runId}")
    public AnalysisStatusResponse getStatus(
            @PathVariable Long runId,
			@CookieValue(name = "GOLDENSTEP_RECOVERY", required = false) String recoveryToken
    ) {
        return analysisRunService.getStatus(runId, recoveryToken);
    }
}