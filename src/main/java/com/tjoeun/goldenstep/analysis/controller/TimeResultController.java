package com.tjoeun.goldenstep.analysis.controller;

import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tjoeun.goldenstep.analysis.dto.response.TimeResultResponse;
import com.tjoeun.goldenstep.analysis.service.AnalysisRunService;
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
    private final AnalysisRunService analysisRunService;

    @Operation(
            summary = "저장된 시간점 결과 조회",
            description = """
		            복구 쿠키로 본인 세션의 분석 실행을 확인합니다.
		            DB에 저장된 해당 시간점의 결과와 추천 장소를 반환합니다.
		            저장된 결과가 없으면 404를 반환하며 새 분석을 실행하지 않습니다.
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
    
    @Operation(
            summary = "시간점별 분석 결과 생성 또는 재사용",
            description = """
                    복구 쿠키로 탐색 세션과 분석 실행의 소유권을 확인합니다.
                    초기 분석이 완료된 경우에만 요청할 수 있습니다.
                    해당 시간점의 결과가 저장되어 있으면 기존 결과를 반환합니다.
                    결과가 없으면 분석과 주소 조회를 진행한 후 저장하여 반환합니다.
                    추가 시간점 분석 실패 시 기존 분석 상태와 저장 결과는 유지됩니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "저장 결과 재사용 또는 새 결과 생성 완료",
                    content = @Content(
							schema = @Schema(implementation = TimeResultResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "시간점 또는 요청값이 올바르지 않음",
                    content = @Content(
							schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "복구 쿠키가 없거나 연결된 탐색 세션이 없음",
                    content = @Content(
							schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "분석 실행이 없거나 다른 세션의 분석 실행",
                    content = @Content(
							schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "초기 분석이 완료되지 않음",
                    content = @Content(
							schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "410",
                    description = "탐색 세션의 보관 기간이 만료됨",
                    content = @Content(
							schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "502",
                    description = "분석 서비스의 응답이 올바르지 않음",
                    content = @Content(
							schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "분석 처리 또는 결과 저장 중 서버 오류",
                    content = @Content(
							schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    @PostMapping("/{runId}/results/{timePoint}")
    public TimeResultResponse createResult(
			@PathVariable Long runId,
			@PathVariable String timePoint,
			@CookieValue(name = "GOLDENSTEP_RECOVERY", required = false) String recoveryToken
    ) {
		analysisRunService.getOrCreateTimePointResult(runId, timePoint, recoveryToken);

		return timeResultService.getStoredResult(runId, timePoint, recoveryToken);
    }
}