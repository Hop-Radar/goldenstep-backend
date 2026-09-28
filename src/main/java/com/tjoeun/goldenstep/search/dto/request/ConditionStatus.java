package com.tjoeun.goldenstep.search.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "상태 선택: NONE=없음, YES=있음, UNKNOWN=알 수 없음. UNKNOWN은 저장 시 NONE으로 처리")
public enum ConditionStatus {
	NONE,
    YES,
    UNKNOWN;

    public ConditionStatus normalized() {
        return this == UNKNOWN ? NONE : this;
    }
}
