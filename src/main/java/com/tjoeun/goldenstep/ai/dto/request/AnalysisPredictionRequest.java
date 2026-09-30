package com.tjoeun.goldenstep.ai.dto.request;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

import com.tjoeun.goldenstep.analysis.entity.TimePoint;

import lombok.Getter;

@Getter
public class AnalysisPredictionRequest {

    private final TimePoint timePoint;
    private final LocalDateTime targetAt;

    private final BigDecimal lastLat;
    private final BigDecimal lastLng;
    private final LocalDateTime lastSeenAt;

    private final Map<String, Object> personAttributes;

    public AnalysisPredictionRequest(
            TimePoint timePoint,
            LocalDateTime targetAt,
            BigDecimal lastLat,
            BigDecimal lastLng,
            LocalDateTime lastSeenAt,
            Map<String, Object> personAttributes
    ) {
        this.timePoint = timePoint;
        this.targetAt = targetAt;
        this.lastLat = lastLat;
        this.lastLng = lastLng;
        this.lastSeenAt = lastSeenAt;
        this.personAttributes = personAttributes;
    }
}