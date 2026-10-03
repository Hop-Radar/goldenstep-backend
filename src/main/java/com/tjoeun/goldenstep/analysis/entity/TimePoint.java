package com.tjoeun.goldenstep.analysis.entity;

import java.time.LocalDateTime;

public enum TimePoint {

    NOW(0),
    AFTER_30M(30),
    AFTER_1H(60),
    AFTER_3H(180),
    AFTER_6H(360);

    private final int offsetMinutes;

    TimePoint(int offsetMinutes) {
        this.offsetMinutes = offsetMinutes;
    }

    public LocalDateTime calculateTargetAt(
            LocalDateTime requestedAt
    ) {
        return requestedAt.plusMinutes(offsetMinutes);
    }
}