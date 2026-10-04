package com.tjoeun.goldenstep.snapshot.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tjoeun.goldenstep.global.exception.ErrorResponse;
import com.tjoeun.goldenstep.snapshot.dto.response.SnapshotResponse;
import com.tjoeun.goldenstep.snapshot.service.SnapshotService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;

@Tag(name = "공유 스냅샷", description = "공유 스냅샷 발급·조회·폐기 API")
@RestController
@RequestMapping("/api/snapshots")
@RequiredArgsConstructor
public class SnapshotController {

    private final SnapshotService snapshotService;

    @Operation(
            summary = "공유 스냅샷 조회",
            description = """
                    복구 쿠키 없이 공유 토큰으로 발급 당시의 탐색 정보를 조회합니다.
                    원본 탐색 세션의 변경·삭제와 관계없이 저장된 스냅샷을 반환합니다.
                    만료되었거나 폐기된 링크는 410을 반환합니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "공유 스냅샷 조회 성공",
                    content = @Content(schema = @Schema(implementation = SnapshotResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "공유 토큰이 올바르지 않거나 스냅샷을 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "410",
                    description = "공유 링크가 만료되었거나 폐기됨",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "서버 처리 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping("/{shareToken}")
    public SnapshotResponse get(
            @Parameter(description = "공유 URL의 token 값 (Base64 URL 형식 43자)")
            @PathVariable("shareToken") String shareToken,
            HttpServletResponse response
    ) {
        response.setHeader("Cache-Control", "no-store");

        return snapshotService.get(shareToken);
    }
}
