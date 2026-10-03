package com.tjoeun.goldenstep.search.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = """
		대상자 분류:
		CHILD=어린아이, TEEN=청소년,
		ADULT_MALE=성인 남성, ADULT_FEMALE=성인 여성,
		OLDER_ADULT=노약자(성별 구분 없음)
		""")
public enum PersonType {
	CHILD,
	TEEN,
	ADULT_MALE,
	ADULT_FEMALE,
	OLDER_ADULT,
}
