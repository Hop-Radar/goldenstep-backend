package com.tjoeun.goldenstep.ai.client;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.tjoeun.goldenstep.ai.dto.request.AnalysisPredictionRequest;
import com.tjoeun.goldenstep.ai.dto.response.AnalysisPredictionResponse;
import com.tjoeun.goldenstep.ai.mapper.FastApiSearchRequestMapper;
import com.tjoeun.goldenstep.analysis.entity.TimePoint;
import com.tjoeun.goldenstep.global.exception.ErrorCode;
import com.tjoeun.goldenstep.global.exception.RestException;

import tools.jackson.databind.json.JsonMapper;

class FastApiAnalysisClientTest {

    private static final String SEARCH_URL =
            "http://localhost:8000/api/v1/simulation/search";

    private MockRestServiceServer server;
    private FastApiAnalysisClient client;
    private JsonMapper jsonMapper;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl("http://localhost:8000");

        server = MockRestServiceServer.bindTo(builder).build();
        jsonMapper = JsonMapper.builder().build();

        client = new FastApiAnalysisClient(
                builder.build(),
                jsonMapper,
                new FastApiSearchRequestMapper()
        );
    }

    @Test
    void FastAPI_규격으로_요청하고_분석_응답을_반환한다()
            throws Exception {

        String responseJson;

        try (var input = new ClassPathResource(
                "mock/mock_search_response.json"
        ).getInputStream()) {
            responseJson = new String(
                    input.readAllBytes(),
                    StandardCharsets.UTF_8
            );
        }

        server.expect(requestTo(SEARCH_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(request -> {
                    MockClientHttpRequest mockRequest =
                            (MockClientHttpRequest) request;

                    Map<?, ?> body = jsonMapper.readValue(
                            mockRequest.getBodyAsString(),
                            Map.class
                    );

                    Map<?, ?> missingPerson =
                            (Map<?, ?>) body.get("missing_person");

                    Map<?, ?> location =
                            (Map<?, ?>) missingPerson.get(
                                    "last_seen_location"
                            );

                    assertAll(
                            () -> assertNotNull(body.get("request_id")),
                            () -> assertEquals(
                                    "DEMENTIA",
                                    missingPerson.get("person_type")
                            ),
                            () -> assertEquals(
                                    "2026-09-30T13:30:00+09:00",
                                    missingPerson.get("last_seen_time")
                            ),
                            () -> assertEquals(
                                    0.5,
                                    ((Number) missingPerson.get(
                                            "elapsed_hours"
                                    )).doubleValue(),
                                    0.000001
                            ),
                            () -> assertEquals(
                                    37.566535,
                                    ((Number) location.get("lat"))
                                            .doubleValue(),
                                    0.000001
                            ),
                            () -> assertEquals(
                                    126.977969,
                                    ((Number) location.get("lon"))
                                            .doubleValue(),
                                    0.000001
                            )
                    );
                })
                .andRespond(withSuccess(
                        responseJson,
                        MediaType.APPLICATION_JSON
                ));

        AnalysisPredictionResponse response =
                client.analyze(request());

        assertAll(
                () -> assertEquals("SUCCESS", response.getStatus()),
                () -> assertEquals(
                        "STABLE",
                        response.getSummary().getReliabilityStatus()
                ),
                () -> assertNotNull(response.getBoundaryZone()),
                () -> assertEquals(
                        3,
                        response.getPriorityPoints().size()
                )
        );

        server.verify();
    }

    @Test
    void FastAPI_HTTP_오류를_공통_예외로_변환한다() {
        server.expect(requestTo(SEARCH_URL))
                .andRespond(withStatus(
                        HttpStatus.INTERNAL_SERVER_ERROR
                ));

        RestException exception = assertThrows(
                RestException.class,
                () -> client.analyze(request())
        );

        assertEquals(
                ErrorCode.AI_ANALYSIS_REQUEST_FAILED,
                exception.getErrorCode()
        );

        server.verify();
    }

    @Test
    void 응답이_올바른_JSON이_아니면_거부한다() {
        server.expect(requestTo(SEARCH_URL))
                .andRespond(withSuccess(
                        "JSON이 아닌 응답",
                        MediaType.TEXT_PLAIN
                ));

        RestException exception = assertThrows(
                RestException.class,
                () -> client.analyze(request())
        );

        assertEquals(
                ErrorCode.INVALID_ANALYSIS_RESPONSE,
                exception.getErrorCode()
        );

        server.verify();
    }

    @Test
    void 응답의_필수_분석_정보가_없으면_거부한다() {
        server.expect(requestTo(SEARCH_URL))
                .andRespond(withSuccess(
                        """
                        {
                          "status": "SUCCESS"
                        }
                        """,
                        MediaType.APPLICATION_JSON
                ));

        RestException exception = assertThrows(
                RestException.class,
                () -> client.analyze(request())
        );

        assertEquals(
                ErrorCode.INVALID_ANALYSIS_RESPONSE,
                exception.getErrorCode()
        );

        server.verify();
    }

    private AnalysisPredictionRequest request() {
        return new AnalysisPredictionRequest(
                TimePoint.NOW,
                LocalDateTime.of(2026, 9, 30, 14, 0),
                new BigDecimal("37.566535"),
                new BigDecimal("126.977969"),
                LocalDateTime.of(2026, 9, 30, 13, 30),
                Map.of(
                        "personType", "OLDER_ADULT",
                        "diseaseStatus", "YES"
                )
        );
    }
}