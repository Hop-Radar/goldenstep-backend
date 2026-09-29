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

    private final Long runId;
    private final Long resultId;
    private final TimePoint timePoint;
    private final LocalDateTime targetAt;
    private final Map<String, Object> boundaryZone;
    private final String reliabilityStatus;
    private final List<PriorityPlaceResponse> places;

	public TimeResultResponse(TimeResult result, List<PriorityPlace> places) {
        this.runId = result.getAnalysisRun().getId();
        this.resultId = result.getId();
        this.timePoint = result.getTimePoint();
        this.targetAt = result.getTargetAt();
        this.boundaryZone = result.getBoundaryZone();
        this.reliabilityStatus = result.getReliabilityStatus();
        this.places = places.stream()
                .map(PriorityPlaceResponse::new)
                .toList();
    }
}