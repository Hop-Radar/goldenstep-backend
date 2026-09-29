package com.tjoeun.goldenstep.analysis.dto.response;

import java.time.LocalDateTime;

import com.tjoeun.goldenstep.analysis.entity.AnalysisRun;
import com.tjoeun.goldenstep.analysis.entity.Status;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "분석 진행 상태 응답")
public class AnalysisStatusResponse {

    @Schema(description = "분석 실행 ID", example = "1")
    private final Long runId;

    @Schema(description = "분석 상태", example = "PROCESSING")
    private final Status status;

    @Schema(description = "분석 요청 시각")
    private final LocalDateTime requestedAt;

    @Schema(description = "분석 종료 시각. 진행 중이면 null")
    private final LocalDateTime completedAt;

    public AnalysisStatusResponse(AnalysisRun run) {
        this.runId = run.getId();
        this.status = run.getStatus();
        this.requestedAt = run.getRequestedAt();
        this.completedAt = run.getCompletedAt();
    }
}