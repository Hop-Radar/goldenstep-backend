package com.tjoeun.goldenstep.search.dto.request;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "탐색 정보 입력 요청")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CreateSearchSessionRequest {
	
	public static final int MIN_AGE_OPTION = 10;
    public static final int MAX_AGE_OPTION = 85;

    @Schema(
            description = "지도에서 확정한 마지막 확인 위치의 위도",
            example = "37.5665000",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "마지막 확인 위치의 위도를 선택해주세요.")
    @DecimalMin(value = "-90", message = "위도는 -90 이상이어야 합니다.")
    @DecimalMax(value = "90", message = "위도는 90 이하여야 합니다.")
    @Digits(integer = 3, fraction = 7, message = "위도는 소수점 아래 7자리까지 입력해주세요.")
    private BigDecimal lastLat;

    @Schema(
            description = "지도에서 확정한 마지막 확인 위치의 경도",
            example = "126.9780000",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "마지막 확인 위치의 경도를 선택해주세요.")
    @DecimalMin(value = "-180", message = "경도는 -180 이상이어야 합니다.")
    @DecimalMax(value = "180", message = "경도는 180 이하여야 합니다.")
    @Digits(integer = 3, fraction = 7, message = "경도는 소수점 아래 7자리까지 입력해주세요.")
    private BigDecimal lastLng;

    @Schema(
            description = "마지막 확인 위치의 주소. 좌표가 확정되었다면 생략 가능",
            example = "서울특별시 중구 세종대로 110",
            maxLength = 255,
            nullable = true
    )
    @Size(max = 255, message = "주소는 255자 이하로 입력해주세요.")
    private String lastAddress;

    @Schema(
            description = "마지막 확인 시각. 한국 시각 기준이며 현재보다 이전, 5분 간격, 초는 00",
            example = "2026-09-28T21:00:00",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "마지막 확인 시각을 선택해주세요.")
    private LocalDateTime lastSeenAt;
    
    @Schema(
            description = "대상자 분류. 노약자는 성별을 구분하지 않음",
            example = "CHILD",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "대상자 분류를 선택해주세요.")
    private PersonType personType;

    @Schema(
            description = "선택 나이. 10은 '10세 이하', 11~84는 해당 나이, 85는 '85세 이상'. 미선택 시 생략",
            example = "10",
            minimum = "10",
            maximum = "85",
            nullable = true
    )
    @Min(value = MIN_AGE_OPTION, message = "나이는 10세 이상부터 선택할 수 있습니다.")
    @Max(value = MAX_AGE_OPTION, message = "나이는 85세 이하까지 선택할 수 있습니다.")
    private Integer age;

    @Schema(
            description = "장애 여부. UNKNOWN은 입력 편의를 위한 값이며 저장 시 NONE으로 처리",
            example = "UNKNOWN",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "장애 여부를 선택해주세요.")
    private ConditionStatus disabilityStatus;

    @Schema(
            description = "질환 여부. UNKNOWN은 입력 편의를 위한 값이며 저장 시 NONE으로 처리",
            example = "NONE",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "질환 여부를 선택해주세요.")
    private ConditionStatus diseaseStatus;

    @Schema(
            description = "신체적 특징에 관한 추가 정보",
            example = "왼쪽 다리를 절음",
            maxLength = 500,
            nullable = true
    )
    @Size(max = 500, message = "신체적 특징은 500자 이하로 입력해주세요.")
    private String physicalFeatures;
}