package com.tjoeun.goldenstep.analysis.event;

import lombok.Getter;

@Getter
public class AnalysisRequestedEvent {

    private final Long runId;

    public AnalysisRequestedEvent(Long runId) {
        this.runId = runId;
    }
}