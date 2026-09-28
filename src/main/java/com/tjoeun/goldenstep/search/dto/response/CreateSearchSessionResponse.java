package com.tjoeun.goldenstep.search.dto.response;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Schema(description = "탐색 정보 저장 결과. 복구 토큰은 본문 대신 쿠키로 발급")
@Getter
public class CreateSearchSessionResponse {

	@Schema(description = "생성된 탐색 세션 ID", example = "1")
	private final Long sessionId;

	@Schema(
			description = "탐색 세션 만료 시각. 생성 시각으로부터 24시간 뒤이며 한국 시각 기준",
			example = "2026-09-29T21:00:00"
	)
	private final LocalDateTime expiresAt;

	public CreateSearchSessionResponse(Long sessionId, LocalDateTime expiresAt) {
		this.sessionId = sessionId;
		this.expiresAt = expiresAt;
	}
}