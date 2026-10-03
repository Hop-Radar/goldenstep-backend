package com.tjoeun.goldenstep.search.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

import com.tjoeun.goldenstep.search.entity.SearchSession;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "복구된 탐색 세션의 입력 정보")
public class CurrentSearchSessionResponse {

    @Schema(description = "탐색 세션 ID", example = "1")
    private final Long sessionId;

    @Schema(description = "마지막 확인 위치의 위도", example = "37.5665000")
    private final BigDecimal lastLat;

    @Schema(description = "마지막 확인 위치의 경도", example = "126.9780000")
    private final BigDecimal lastLng;

    @Schema(description = "마지막 확인 위치의 주소")
    private final String lastAddress;

    @Schema(description = "마지막 확인 시각. 한국 시각 기준")
    private final LocalDateTime lastSeenAt;

    @Schema(description = "저장된 대상자 정보. UNKNOWN은 NONE으로 정규화된 값")
    private final Map<String, Object> personAttributes;

    @Schema(description = "세션 생성 시각. 한국 시각 기준")
    private final LocalDateTime createdAt;

    @Schema(description = "세션 만료 시각. 한국 시각 기준")
    private final LocalDateTime expiresAt;

    public CurrentSearchSessionResponse(SearchSession session) {
        this.sessionId = session.getId();
        this.lastLat = session.getLastLat();
        this.lastLng = session.getLastLng();
        this.lastAddress = session.getLastAddress();
        this.lastSeenAt = session.getLastSeenAt();
        this.personAttributes = session.getPersonAttributes();
        this.createdAt = session.getCreatedAt();
        this.expiresAt = session.getExpiresAt();
    }
}