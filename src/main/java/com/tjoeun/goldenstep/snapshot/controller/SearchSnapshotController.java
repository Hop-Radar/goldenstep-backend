package com.tjoeun.goldenstep.snapshot.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.tjoeun.goldenstep.global.exception.ErrorResponse;
import com.tjoeun.goldenstep.snapshot.dto.request.CreateSnapshotRequest;
import com.tjoeun.goldenstep.snapshot.dto.response.CreateSnapshotResponse;
import com.tjoeun.goldenstep.snapshot.service.SnapshotService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

@Tag(name = "공유 스냅샷", description = "공유 스냅샷 발급·조회·폐기 API")
@RestController
@RequestMapping("/api/search/snapshots")
@RequiredArgsConstructor
public class SearchSnapshotController {

    private final SnapshotService snapshotService;

    @Operation(
            summary = "공유 스냅샷 발급",
            description = """
                    복구 쿠키로 분석 실행의 소유권을 확인하고 저장된 시간점 결과를 복사합니다.
                    위치·추가 정보·지도 경계·장소 확인 상태는 발급 당시 값으로 유지됩니다.
                    새 AI 분석을 실행하지 않으며, 링크는 발급 후 12시간 동안 조회할 수 있습니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "공유 스냅샷 발급 성공",
                    content = @Content(schema = @Schema(implementation = CreateSnapshotResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "요청값 또는 시간점이 올바르지 않음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "복구 쿠키가 없거나 연결된 탐색 세션이 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "현재 세션의 분석 실행 또는 저장된 결과를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "410",
                    description = "탐색 세션 보관 기간 만료",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "서버 처리 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreateSnapshotResponse create(
            @Valid @RequestBody CreateSnapshotRequest request,
            @Parameter(name = "GOLDENSTEP_RECOVERY", description = "탐색 정보 입력 시 발급된 복구 쿠키", in = ParameterIn.COOKIE)
            @CookieValue(name = "GOLDENSTEP_RECOVERY", required = false) String recoveryToken,
            HttpServletResponse response
    ) {
        response.setHeader("Cache-Control", "no-store");

        return snapshotService.create(request, recoveryToken);
    }

    @Operation(
            summary = "공유 링크 폐기",
            description = """
                    스냅샷을 발급한 탐색 세션에서만 링크를 폐기할 수 있습니다.
                    이미 폐기한 링크는 기존 폐기 시각을 유지하고 성공으로 처리합니다.
                    공유 링크만 가진 조회자는 폐기할 수 없습니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "204",
                    description = "공유 링크 폐기 완료",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "스냅샷 ID가 정수가 아님",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "복구 쿠키가 없거나 연결된 탐색 세션이 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "현재 세션에서 발급한 스냅샷을 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "410",
                    description = "탐색 세션 보관 기간 만료",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "서버 처리 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @DeleteMapping("/{snapshotId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revoke(
            @Parameter(description = "발급 응답의 공유 스냅샷 ID", example = "1")
            @PathVariable("snapshotId") Long snapshotId,
            @Parameter(name = "GOLDENSTEP_RECOVERY", description = "탐색 정보 입력 시 발급된 복구 쿠키", in = ParameterIn.COOKIE)
            @CookieValue(name = "GOLDENSTEP_RECOVERY", required = false) String recoveryToken
    ) {
        snapshotService.revoke(snapshotId, recoveryToken);
    }
}
