package com.tjoeun.goldenstep.snapshot.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import com.tjoeun.goldenstep.analysis.entity.TimePoint;
import com.tjoeun.goldenstep.snapshot.entity.Snapshot;
import com.tjoeun.goldenstep.snapshot.entity.SnapshotPlace;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Getter;

@Getter
@Schema(description = "공유 당시 탐색 정보")
public class SnapshotResponse {

    @Schema(description = "마지막 확인 위치 위도", example = "37.5665000")
    private final BigDecimal latitude;

    @Schema(description = "마지막 확인 위치 경도", example = "126.9780000")
    private final BigDecimal longitude;

    @Schema(description = "마지막 확인 위치 주소", example = "서울특별시 중구")
    private final String address;

    @Schema(description = "마지막 확인 시각 (한국 시각)", example = "2026-10-04T11:00:00")
    private final LocalDateTime lastSeenTime;

    @Schema(description = "공유 당시 추가 정보. 없으면 null", example = "파란 옷")
    private final String additionalInfo;

    @Schema(description = "공유한 분석 시간점", example = "AFTER_1H")
    private final TimePoint timePoint;

    @Schema(description = "선택한 분석 시간대 (0, 30, 60, 180, 360분)", example = "60")
    private final int selectedMinutes;

    @Schema(description = "공유 당시 GeoJSON 탐색 경계. 없으면 null")
    private final Map<String, Object> boundaryZone;

    @Schema(description = "추천 순위순 공유 장소 목록")
    private final List<SnapshotPlaceResponse> priorityPlaces;

    @Schema(description = "공유 링크 발급 시각 (한국 시각)", example = "2026-10-04T12:00:00")
    private final LocalDateTime createdAt;

    @Schema(description = "공유 링크 만료 시각 (한국 시각)", example = "2026-10-05T00:00:00")
    private final LocalDateTime expiresAt;

    public SnapshotResponse(
            BigDecimal latitude,
            BigDecimal longitude,
            String address,
            LocalDateTime lastSeenTime,
            String additionalInfo,
            TimePoint timePoint,
            int selectedMinutes,
            Map<String, Object> boundaryZone,
            List<SnapshotPlaceResponse> priorityPlaces,
            LocalDateTime createdAt,
            LocalDateTime expiresAt
    ) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.address = address;
        this.lastSeenTime = lastSeenTime;
        this.additionalInfo = additionalInfo;
        this.timePoint = timePoint;
        this.selectedMinutes = selectedMinutes;
        this.boundaryZone = boundaryZone;
        this.priorityPlaces = priorityPlaces;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public SnapshotResponse(Snapshot snapshot, List<SnapshotPlace> places) {
        this(
                snapshot.getLastLat(), snapshot.getLastLng(), snapshot.getLastAddress(),
                snapshot.getLastSeenAt(), snapshot.getAdditionalInfo(), snapshot.getTimePoint(),
                selectedMinutes(snapshot.getTimePoint()), snapshot.getBoundaryZone(),
                places.stream().map(SnapshotPlaceResponse::new).toList(),
                snapshot.getCreatedAt(), snapshot.getExpiresAt()
        );
    }

    private static int selectedMinutes(TimePoint timePoint) {
        if (timePoint == null) {
            return 0;
        }

        return switch (timePoint) {
            case NOW -> 0;
            case AFTER_30M -> 30;
            case AFTER_1H -> 60;
            case AFTER_3H -> 180;
            case AFTER_6H -> 360;
        };
    }
}
