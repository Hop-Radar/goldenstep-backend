package com.tjoeun.goldenstep.search.dto.response;

import java.time.LocalDateTime;

import com.tjoeun.goldenstep.analysis.entity.Status;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Schema(description = "탐색 정보 저장 결과. 복구 토큰은 본문 대신 쿠키로 발급")
@Getter
public class CreateSearchSessionResponse {

	@Schema(description = "생성된 탐색 세션 ID", example = "1")
	private final Long sessionId;

	@Schema(description = "분석 실행 ID", example = "1")
	private final Long runId;

	@Schema(description = "분석 상태", example = "PROCESSING")
	private final Status status;

	@Schema(description = "탐색 세션 만료 시각. 생성 시각으로부터 24시간 뒤")
	private final LocalDateTime expiresAt;

	public CreateSearchSessionResponse(
            Long sessionId,
            Long runId,
            Status status,
            LocalDateTime expiresAt
    ) {
        this.sessionId = sessionId;
        this.runId = runId;
        this.status = status;
        this.expiresAt = expiresAt;
    }
}