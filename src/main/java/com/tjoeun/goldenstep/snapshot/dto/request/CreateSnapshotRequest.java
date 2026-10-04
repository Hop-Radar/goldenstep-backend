package com.tjoeun.goldenstep.snapshot.dto.request;

import com.tjoeun.goldenstep.analysis.entity.TimePoint;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Schema(description = "공유할 저장된 분석 결과")
public class CreateSnapshotRequest {

    @NotNull(message = "분석 실행 ID를 입력해주세요.")
    @Positive(message = "분석 실행 ID는 1 이상의 정수여야 합니다.")
    @Schema(description = "현재 탐색 세션에 속한 분석 실행 ID", example = "1",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private Long runId;

    @NotNull(message = "공유할 분석 시간점을 선택해주세요.")
    @Schema(description = "공유할 저장된 분석 시간점", example = "AFTER_1H",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private TimePoint timePoint;

    public CreateSnapshotRequest(Long runId, TimePoint timePoint) {
        this.runId = runId;
        this.timePoint = timePoint;
    }
}
