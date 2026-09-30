package com.tjoeun.goldenstep.ai.client;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;

import com.tjoeun.goldenstep.ai.dto.request.AnalysisPredictionRequest;
import com.tjoeun.goldenstep.ai.dto.response.AnalysisPredictionResponse;
import com.tjoeun.goldenstep.ai.dto.response.AnalysisPredictionResponse.PriorityPoint;
import com.tjoeun.goldenstep.analysis.entity.TimePoint;

import tools.jackson.databind.json.JsonMapper;

class MockAnalysisClientTest {

    private MockAnalysisClient client;

    @BeforeEach
    void setUp() {
        client = new MockAnalysisClient(
                JsonMapper.builder().build(),
                new ClassPathResource(
                        "mock/mock_search_response.json"
                )
        );
    }

    @Test
    void 모의_JSON을_분석_응답으로_변환한다() {
        AnalysisPredictionResponse response =
                client.analyze(createRequest());

        assertAll(
                () -> assertEquals(
                        "SUCCESS",
                        response.getStatus()
                ),
                () -> assertNotNull(response.getSummary()),
                () -> assertNotNull(response.getBoundaryZone()),
                () -> assertNotNull(response.getPriorityPoints())
        );

        assertAll(
                () -> assertEquals(
                        "STABLE",
                        response.getSummary()
                                .getReliabilityStatus()
                ),
                () -> assertEquals(
                        "FeatureCollection",
                        response.getBoundaryZone().get("type")
                ),
                () -> assertEquals(
                        3,
                        response.getPriorityPoints().size()
                )
        );

        PriorityPoint first =
                response.getPriorityPoints().get(0);

        assertNotNull(first.getLocation());

        assertAll(
                () -> assertEquals(
                        Byte.valueOf((byte) 1),
                        first.getRank()
                ),
                () -> assertEquals(
                        "poi_001",
                        first.getPoiId()
                ),
                () -> assertEquals(
                        "시청역 1호선 5번 출구 앞",
                        first.getName()
                ),
                () -> assertEquals(
                        0,
                        new BigDecimal("37.565812").compareTo(
                                first.getLocation().getLat()
                        )
                ),
                () -> assertEquals(
                        0,
                        new BigDecimal("126.977234").compareTo(
                                first.getLocation().getLon()
                        )
                ),
                () -> assertEquals(
                        0,
                        new BigDecimal("0.892").compareTo(
                                first.getScore()
                        )
                )
        );
    }

    @Test
    void 파일이_없으면_로딩_실패_예외가_발생한다() {
        MockAnalysisClient missingFileClient =
                new MockAnalysisClient(
                        JsonMapper.builder().build(),
                        new ClassPathResource(
                                "mock/not-existing-response.json"
                        )
                );

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> missingFileClient.analyze(createRequest())
        );

        assertTrue(
                exception.getMessage().contains(
                        "모의 분석 응답 파일을 읽지 못했습니다."
                )
        );

        assertNotNull(exception.getCause());
    }

    @Test
    void 필수_결과가_없으면_응답을_거부한다() {
        String invalidJson = """
                {
                    "status": "SUCCESS",
                    "summary": {
                        "reliability_status": "STABLE"
                    }
                }
                """;

        MockAnalysisClient invalidResponseClient =
                new MockAnalysisClient(
                        JsonMapper.builder().build(),
                        new ByteArrayResource(
                                invalidJson.getBytes(
                                        StandardCharsets.UTF_8
                                )
                        )
                );

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> invalidResponseClient.analyze(
                        createRequest()
                )
        );

        assertTrue(
                exception.getMessage().contains(
                        "priority_points가 없습니다."
                )
        );
    }

    private AnalysisPredictionRequest createRequest() {
        LocalDateTime requestedAt =
                LocalDateTime.of(2026, 9, 30, 15, 0);

        return new AnalysisPredictionRequest(
                TimePoint.NOW,
                requestedAt,
                new BigDecimal("37.566535"),
                new BigDecimal("126.977969"),
                requestedAt.minusHours(1),
                Map.of("personType", "CHILD")
        );
    }
}