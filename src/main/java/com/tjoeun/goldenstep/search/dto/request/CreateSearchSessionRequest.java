package com.tjoeun.goldenstep.search.dto.request;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CreateSearchSessionRequest {
	
	public static final int MIN_AGE_OPTION = 10;
    public static final int MAX_AGE_OPTION = 85;

    @NotNull(message = "마지막 확인 위치의 위도를 선택해주세요.")
    @DecimalMin(value = "-90", message = "위도는 -90 이상이어야 합니다.")
    @DecimalMax(value = "90", message = "위도는 90 이하여야 합니다.")
    @Digits(integer = 3, fraction = 7, message = "위도는 소수점 아래 7자리까지 입력해주세요.")
    private BigDecimal lastLat;

    @NotNull(message = "마지막 확인 위치의 경도를 선택해주세요.")
    @DecimalMin(value = "-180", message = "경도는 -180 이상이어야 합니다.")
    @DecimalMax(value = "180", message = "경도는 180 이하여야 합니다.")
    @Digits(integer = 3, fraction = 7, message = "경도는 소수점 아래 7자리까지 입력해주세요.")
    private BigDecimal lastLng;

    @Size(max = 255, message = "주소는 255자 이하로 입력해주세요.")
    private String lastAddress;

    @NotNull(message = "마지막 확인 시각을 선택해주세요.")
    private LocalDateTime lastSeenAt;

    @NotNull(message = "대상자 분류를 선택해주세요.")
    private PersonType personType;

    @Min(value = MIN_AGE_OPTION, message = "나이는 10세 이상부터 선택할 수 있습니다.")
    @Max(value = MAX_AGE_OPTION, message = "나이는 85세 이하까지 선택할 수 있습니다.")
    private Integer age;

    @NotNull(message = "장애 여부를 선택해주세요.")
    private ConditionStatus disabilityStatus;

    @NotNull(message = "질환 여부를 선택해주세요.")
    private ConditionStatus diseaseStatus;

    @Size(max = 500, message = "신체적 특징은 500자 이하로 입력해주세요.")
    private String physicalFeatures;
}