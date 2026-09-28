package com.tjoeun.goldenstep.search.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.tjoeun.goldenstep.global.exception.ErrorResponse;
import com.tjoeun.goldenstep.search.dto.request.CreateSearchSessionRequest;
import com.tjoeun.goldenstep.search.dto.response.CreateSearchSessionResponse;
import com.tjoeun.goldenstep.search.handler.SearchSessionCookieHandler;
import com.tjoeun.goldenstep.search.service.SearchSessionService;
import com.tjoeun.goldenstep.search.service.SearchSessionService.CreateResult;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "탐색 정보", description = "실종자 탐색 정보 API")
@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchSessionController {

    private final SearchSessionService searchSessionService;
    private final SearchSessionCookieHandler cookieHandler;

    @Operation(
            summary = "탐색 정보 입력",
            description = """
                    마지막 확인 위치·시각과 대상자 정보를 저장합니다.
                    성공하면 24시간 유효한 복구 토큰을 HttpOnly 쿠키로 발급합니다.
                    이 요청은 분석을 시작하지 않습니다.
                    """
    )
    @ApiResponses({
        @ApiResponse(
                responseCode = "201",
                description = "탐색 정보 저장 완료",
					content = @Content(schema = @Schema(implementation = CreateSearchSessionResponse.class)),
                headers = @Header(
                        name = "Set-Cookie",
                        description = "GOLDENSTEP_RECOVERY 복구 쿠키. HttpOnly, SameSite=Lax, Max-Age=86400"
                )
        ),
        @ApiResponse(
                responseCode = "400",
                description = "입력값 누락, 형식 오류 또는 마지막 확인 시각 오류",
					content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
                responseCode = "500",
                description = "서버 처리 오류",
					content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
})
    @PostMapping("/input")
    @ResponseStatus(HttpStatus.CREATED)
    public CreateSearchSessionResponse create(
            @Valid @RequestBody CreateSearchSessionRequest request,
            HttpServletResponse response
    ) {
        CreateResult result = searchSessionService.create(request);

        cookieHandler.writeRecoveryCookie(
                response,
                result.getRecoveryToken()
        );

        return new CreateSearchSessionResponse(
                result.getSessionId(),
                result.getExpiresAt()
        );
    }
}