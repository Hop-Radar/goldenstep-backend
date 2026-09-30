package com.tjoeun.goldenstep.global.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {

	// 공통 요청 오류
	INVALID_INPUT(400, "G001", "입력값이 올바르지 않습니다. 표시된 항목을 확인해주세요."),
	REQUEST_BODY_MISSING(400, "G002", "요청 내용이 비어 있습니다. 입력 정보를 함께 보내주세요."),
	INVALID_REQUEST_BODY(400, "G003", "요청 내용을 읽을 수 없습니다. 입력값의 형식을 확인해주세요."),
	MISSING_REQUEST_PARAMETER(400, "G004", "요청에 필요한 항목이 없습니다."),
	INTERNAL_SERVER_ERROR(500, "G999", "서버에서 요청을 처리하는 중 오류가 발생했습니다."),

	// 실종 정보 입력 오류
	INVALID_LAST_SEEN_AT(400, "S001", "마지막 확인 시각은 현재 시각보다 이전이어야 합니다."),
	INVALID_MINUTE_INTERVAL(400, "S002", "마지막 확인 시각의 분은 5분 간격으로 선택해주세요."),
	INVALID_LOCATION(400, "S003", "마지막 확인 위치의 좌표가 올바르지 않습니다. 지도에서 다시 선택해주세요."),

	// 탐색 세션 오류: 쿠키를 이용한 세션 조회 API에서 사용
	RECOVERY_TOKEN_MISSING(401, "S005", "탐색 정보를 확인할 수 없습니다. 이 브라우저에서 탐색을 다시 시작해주세요."),
	SEARCH_SESSION_NOT_FOUND(401, "S006", "이 브라우저에 연결된 탐색 정보를 찾을 수 없습니다. 탐색을 다시 시작해주세요."),
	SEARCH_SESSION_EXPIRED(410, "S007", "탐색 정보의 보관 기간이 끝났습니다. 탐색을 다시 시작해주세요."),
	
	// 분석 결과 오류
	ANALYSIS_RUN_NOT_FOUND(404, "A001", "요청한 분석 실행을 찾을 수 없습니다."),
	INVALID_TIME_POINT(400, "A002", "지원하지 않는 시간점입니다. NOW, AFTER_30M, AFTER_1H, AFTER_3H, AFTER_6H 중에서 선택해 주세요."),
	TIME_RESULT_NOT_FOUND(404, "A003", "요청한 시간점의 분석 결과가 아직 저장되지 않았습니다."),
	
	// 분석 과정 중 오류
	ANALYSIS_REQUEST_MISSING(500, "A010", "분석 요청 정보를 구성하지 못했습니다."),
	MOCK_ANALYSIS_RESPONSE_LOAD_FAILED(500, "A011", "개발용 분석 응답 파일을 읽지 못했습니다. 파일 위치와 JSON 형식을 확인해주세요."),
	INVALID_ANALYSIS_RESPONSE(502, "A012", "분석 서비스의 응답에 필요한 정보가 없거나 값이 올바르지 않습니다."),
	INVALID_INITIAL_ANALYSIS_STATUS(409, "A013", "현재 시간점 분석은 진행 중인 분석 실행에서만 처리할 수 있습니다."),
	INVALID_ANALYSIS_COMPLETION_TIME(500, "A014", "분석 종료 시각을 올바르게 설정하지 못했습니다."),
	
	// 네이버 검색 API 오류
	PLACE_SEARCH_FAILED(502, "M001", "장소 검색 서비스에서 결과를 가져오지 못했습니다. 잠시 후 다시 시도해주세요."),
	INVALID_PLACE_SEARCH_QUERY(400, "M002", "장소 검색어는 2자 이상 100자 이하로 입력해주세요."),
	ADDRESS_COORDINATES_MISSING(400, "M003", "주소 조회에 필요한 위도와 경도를 모두 입력해주세요."),
	INVALID_ADDRESS_COORDINATES(400, "M004", "주소 조회 위도는 -90~90, 경도는 -180~180이어야 합니다."),
	;

	private final int status;
	private final String divisionCode;
	private final String message;

	ErrorCode(int status, String divisionCode, String message) {
		this.status = status;
		this.divisionCode = divisionCode;
		this.message = message;
	}
}