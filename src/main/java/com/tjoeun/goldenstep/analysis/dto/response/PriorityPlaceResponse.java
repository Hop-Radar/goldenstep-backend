package com.tjoeun.goldenstep.analysis.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.tjoeun.goldenstep.analysis.entity.PriorityPlace;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "추천 장소 및 확인 완료 정보")
public class PriorityPlaceResponse {

    @Schema(description = "추천 장소 ID", example = "1")
    private final Long placeId;

    @Schema(description = "장소 식별자", example = "poi_001")
    private final String poiId;

    @Schema(
            description = "추천 순위",
            example = "1",
            minimum = "1",
            maximum = "3"
    )
    private final Byte priorityRank;

    @Schema(
            description = """
                    추천 장소명.
                    FastAPI 이름이 내부 식별자 또는 미상 값이면
                    네이버 Geocoding의 BUILDING_NAME longName으로 보정합니다.
                    건물명 조회 실패 시 주소를 사용하며,
                    주소도 없으면 좌표를 표시합니다.
                    """,
            example = "신세계백화점",
            maxLength = 255
    )
    private final String name;

    @Schema(
            description = "장소 주소. 주소 조회에 실패하면 null",
            example = "서울특별시 중구 세종대로 101",
            nullable = true
    )
    private final String address;

    @Schema(
            description = "장소 위도",
            example = "37.5658120",
            minimum = "-90",
            maximum = "90"
    )
    private final BigDecimal lat;

    @Schema(
            description = "장소 경도",
            example = "126.9772340",
            minimum = "-180",
            maximum = "180"
    )
    private final BigDecimal lng;

    @Schema(
            description = "AI 추천 점수",
            example = "0.8920000",
            nullable = true
    )
    private final BigDecimal score;

    @Schema(
            description = "추천 장소 확인 완료 여부",
            example = "false"
    )
    private final boolean checked;

    @Schema(
            description = "최초 확인 완료 시각. 미확인 장소는 null",
            example = "2026-09-30T14:15:00",
            nullable = true
    )
    private final LocalDateTime checkedAt;

    public PriorityPlaceResponse(PriorityPlace place) {
        this(place, null);
    }

	public PriorityPlaceResponse(PriorityPlace place, LocalDateTime checkedAt) {
        this.placeId = place.getId();
        this.poiId = place.getPoiId();
        this.priorityRank = place.getPriorityRank();
        this.name = place.getName();
        this.address = place.getAddress();
        this.lat = place.getLat();
        this.lng = place.getLng();
        this.score = place.getScore();
        this.checked = checkedAt != null;
        this.checkedAt = checkedAt;
    }
}