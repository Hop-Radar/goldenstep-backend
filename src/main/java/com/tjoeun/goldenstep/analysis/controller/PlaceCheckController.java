package com.tjoeun.goldenstep.analysis.controller;

import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tjoeun.goldenstep.analysis.dto.response.PlaceCheckResponse;
import com.tjoeun.goldenstep.analysis.entity.PlaceCheck;
import com.tjoeun.goldenstep.analysis.service.PlaceCheckService;
import com.tjoeun.goldenstep.global.exception.ErrorResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "장소 확인", description = "추천 장소 확인 완료 API")
@RestController
@RequestMapping("/api/search/places")
@RequiredArgsConstructor
public class PlaceCheckController {

    private final PlaceCheckService placeCheckService;

    @Operation(
            summary = "추천 장소 확인 완료",
            description = """
                    현재 탐색 세션의 추천 장소를 확인 완료로 표시합니다.
                    이미 확인한 장소는 기존 확인 기록과 최초 확인 시각을 반환합니다.
                    확인 완료로 새로운 분석이나 추천을 실행하지 않습니다.
                    GOLDENSTEP_RECOVERY 복구 쿠키가 필요합니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "확인 완료 또는 기존 확인 기록 반환",
                    content = @Content(
							schema = @Schema(implementation = PlaceCheckResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "장소 ID가 정수가 아니거나 1보다 작음",
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
                    description = "현재 탐색 세션에서 추천 장소를 찾을 수 없음",
                    content = @Content(
							schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "410",
                    description = "탐색 세션의 24시간 보관 기간이 끝남",
                    content = @Content(
							schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "서버 처리 오류",
                    content = @Content(
							schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    @PostMapping("/{placeId}/check")
    public PlaceCheckResponse check(
			@Parameter(description = "분석 결과에 포함된 추천 장소 ID", example = "1")
            @PathVariable Long placeId,
			@Parameter(name = "GOLDENSTEP_RECOVERY", description = "탐색 정보 입력 시 발급된 복구 쿠키", in = ParameterIn.COOKIE)
			@CookieValue(name = "GOLDENSTEP_RECOVERY", required = false) String recoveryToken
    ) {
		PlaceCheck placeCheck = placeCheckService.check(placeId, recoveryToken);

        return new PlaceCheckResponse(placeCheck);
    }
}