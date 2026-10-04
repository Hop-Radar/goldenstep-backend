package com.tjoeun.goldenstep.snapshot.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.tjoeun.goldenstep.snapshot.entity.SnapshotPlace;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Getter;

@Getter
@Schema(description = "공유 당시 추천 장소 및 확인 완료 정보")
public class SnapshotPlaceResponse {

    @Schema(description = "공유 당시 추천 순위", example = "1")
    private final Byte priorityRank;

    @Schema(description = "추천 장소명", example = "우선 확인 장소")
    private final String name;

    @Schema(description = "추천 장소 주소", example = "서울특별시 중구")
    private final String address;

    @Schema(description = "장소 위도", example = "37.5665000")
    private final BigDecimal lat;

    @Schema(description = "장소 경도", example = "126.9780000")
    private final BigDecimal lng;

    @Schema(description = "공유 당시 장소 확인 완료 여부", example = "true")
    private final boolean checked;

    @Schema(description = "공유 당시 최초 확인 완료 시각. 미확인 장소는 null", example = "2026-10-04T11:50:00")
    private final LocalDateTime checkedAt;

    public SnapshotPlaceResponse(
            Byte priorityRank,
            String name,
            String address,
            BigDecimal lat,
            BigDecimal lng,
            boolean checked,
            LocalDateTime checkedAt
    ) {
        this.priorityRank = priorityRank;
        this.name = name;
        this.address = address;
        this.lat = lat;
        this.lng = lng;
        this.checked = checked;
        this.checkedAt = checkedAt;
    }

    public SnapshotPlaceResponse(SnapshotPlace place) {
        this(
                place.getPriorityRank(), place.getName(), place.getAddress(),
                place.getLat(), place.getLng(), place.getCheckedAt() != null, place.getCheckedAt()
        );
    }
}
