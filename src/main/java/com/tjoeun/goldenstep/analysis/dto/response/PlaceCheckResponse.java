package com.tjoeun.goldenstep.analysis.dto.response;

import java.time.LocalDateTime;

import com.tjoeun.goldenstep.analysis.entity.PlaceCheck;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "추천 장소 확인 완료 응답")
public class PlaceCheckResponse {

    @Schema(
            description = "확인 기록 ID",
            example = "1"
    )
    private final Long checkId;

    @Schema(
            description = "확인한 추천 장소 ID",
            example = "3"
    )
    private final Long placeId;

    @Schema(
            description = "장소 확인 완료 여부",
            example = "true"
    )
    private final boolean checked;

    @Schema(
            description = "최초 확인 완료 시각",
            example = "2026-09-30T14:15:00"
    )
    private final LocalDateTime checkedAt;

    public PlaceCheckResponse(PlaceCheck placeCheck) {
        this.checkId = placeCheck.getId();
        this.placeId = placeCheck.getPriorityPlace().getId();
        this.checked = true;
        this.checkedAt = placeCheck.getCheckedAt();
    }
}