package com.tjoeun.goldenstep.analysis.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import com.tjoeun.goldenstep.analysis.entity.PriorityPlace;
import com.tjoeun.goldenstep.analysis.entity.TimePoint;
import com.tjoeun.goldenstep.analysis.entity.TimeResult;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "저장된 시간점별 분석 결과")
public class TimeResultResponse {

    @Schema(description = "분석 실행 ID", example = "2")
    private final Long runId;

    @Schema(description = "시간점 결과 ID", example = "1")
    private final Long resultId;

    @Schema(description = "분석 시간점", example = "NOW")
    private final TimePoint timePoint;

	@Schema(description = "예측 대상 시각", example = "2026-09-30T12:36:59")
    private final LocalDateTime targetAt;

	@Schema(description = "GeoJSON 형식의 AI 예측 영역", nullable = true)
    private final Map<String, Object> boundaryZone;

    @Schema(
            description = "AI 신뢰도 상태",
            example = "STABLE",
            nullable = true
    )
    private final String reliabilityStatus;

    @Schema(description = "추천 순위순 장소 목록과 장소별 확인 완료 정보")
    private final List<PriorityPlaceResponse> places;

    public TimeResultResponse(
            TimeResult result,
            List<PriorityPlace> places,
            Map<Long, LocalDateTime> checkedAtByPlaceId
    ) {
        this.runId = result.getAnalysisRun().getId();
        this.resultId = result.getId();
        this.timePoint = result.getTimePoint();
        this.targetAt = result.getTargetAt();
        this.boundaryZone = result.getBoundaryZone();
        this.reliabilityStatus = result.getReliabilityStatus();

        this.places = places.stream()
                .map(place -> new PriorityPlaceResponse(
                        place,
                        checkedAtByPlaceId.get(place.getId())
                ))
                .toList();
    }
}