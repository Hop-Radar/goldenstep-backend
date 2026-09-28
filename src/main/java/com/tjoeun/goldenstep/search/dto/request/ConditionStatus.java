package com.tjoeun.goldenstep.search.dto.request;

public enum ConditionStatus {
	NONE,
    YES,
    UNKNOWN;

    public ConditionStatus normalized() {
        return this == UNKNOWN ? NONE : this;
    }
}
