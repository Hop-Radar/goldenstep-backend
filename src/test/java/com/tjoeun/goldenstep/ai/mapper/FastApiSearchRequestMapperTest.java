package com.tjoeun.goldenstep.ai.mapper;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.tjoeun.goldenstep.ai.dto.request.AnalysisPredictionRequest;
import com.tjoeun.goldenstep.ai.dto.request.FastApiSearchRequest;
import com.tjoeun.goldenstep.analysis.entity.TimePoint;
import com.tjoeun.goldenstep.global.exception.ErrorCode;
import com.tjoeun.goldenstep.global.exception.RestException;

class FastApiSearchRequestMapperTest {

	private final FastApiSearchRequestMapper mapper = new FastApiSearchRequestMapper();

    @Test
    void 노약자와_질환_있음을_DEMENTIA로_변환한다() {
        Map<String, Object> attributes = Map.of(
                "personType", "OLDER_ADULT",
                "diseaseStatus", "YES"
        );

        FastApiSearchRequest result = mapper.map(
                request(attributes, lastSeenAt().plusMinutes(30))
        );

        assertAll(
                () -> assertEquals(
                        "DEMENTIA",
                        result.getMissingPerson().getPersonType()
                ),
                () -> assertEquals(
                        "OLDER_ADULT",
                        attributes.get("personType")
                ),
                () -> assertEquals(
                        0.5,
                        result.getMissingPerson().getElapsedHours(),
                        0.000001
                ),
                () -> assertEquals(
                        "2026-09-30T13:30:00+09:00",
                        result.getMissingPerson().getLastSeenTime()
                ),
                () -> assertNotNull(result.getRequestId())
        );
    }

    @Test
    void 나머지_입력은_대상자_유형을_그대로_전달한다() {
        FastApiSearchRequest child = mapper.map(
                request(
                        Map.of(
                                "personType", "CHILD",
                                "diseaseStatus", "YES"
                        ),
                        lastSeenAt().plusHours(1)
                )
        );

        FastApiSearchRequest olderAdult = mapper.map(
                request(
                        Map.of(
                                "personType", "OLDER_ADULT",
                                "diseaseStatus", "NO"
                        ),
                        lastSeenAt().plusHours(1)
                )
        );

        assertAll(
                () -> assertEquals(
                        "CHILD",
                        child.getMissingPerson().getPersonType()
                ),
                () -> assertEquals(
                        "OLDER_ADULT",
                        olderAdult.getMissingPerson().getPersonType()
                )
        );
    }

    @Test
    void 짧은_경과_시간도_소수_시간으로_계산한다() {
        FastApiSearchRequest result = mapper.map(
                request(
                        attributes(),
                        lastSeenAt().plusMinutes(5)
                )
        );

        assertEquals(
                5.0 / 60.0,
                result.getMissingPerson().getElapsedHours(),
                0.000001
        );
    }

    @Test
    void 경과_시간이_0이거나_음수이면_거부한다() {
        for (LocalDateTime targetAt : new LocalDateTime[] {
                lastSeenAt(),
                lastSeenAt().minusMinutes(1)
        }) {
            RestException exception = assertThrows(
                    RestException.class,
                    () -> mapper.map(request(attributes(), targetAt))
            );

            assertEquals(
                    ErrorCode.INVALID_ANALYSIS_ELAPSED_TIME,
                    exception.getErrorCode()
            );
        }
    }

    @Test
    void 필수_대상자_정보가_없으면_거부한다() {
        RestException exception = assertThrows(
                RestException.class,
                () -> mapper.map(
                        request(
                                Map.of("personType", "OLDER_ADULT"),
                                lastSeenAt().plusHours(1)
                        )
                )
        );

        assertEquals(
                ErrorCode.INVALID_ANALYSIS_REQUEST,
                exception.getErrorCode()
        );
    }

    private AnalysisPredictionRequest request(
            Map<String, Object> attributes,
            LocalDateTime targetAt
    ) {
        return new AnalysisPredictionRequest(
                TimePoint.NOW,
                targetAt,
                new BigDecimal("37.566535"),
                new BigDecimal("126.977969"),
                lastSeenAt(),
                attributes
        );
    }

    private Map<String, Object> attributes() {
        return Map.of(
                "personType", "OLDER_ADULT",
                "diseaseStatus", "YES"
        );
    }

    private LocalDateTime lastSeenAt() {
        return LocalDateTime.of(2026, 9, 30, 13, 30);
    }
}