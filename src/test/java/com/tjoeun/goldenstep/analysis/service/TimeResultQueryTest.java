package com.tjoeun.goldenstep.analysis.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.tjoeun.goldenstep.analysis.dto.response.TimeResultResponse;
import com.tjoeun.goldenstep.analysis.entity.AnalysisRun;
import com.tjoeun.goldenstep.analysis.entity.TimePoint;
import com.tjoeun.goldenstep.analysis.repository.AnalysisRunRepository;
import com.tjoeun.goldenstep.global.exception.ErrorCode;
import com.tjoeun.goldenstep.global.exception.RestException;
import com.tjoeun.goldenstep.search.entity.SearchSession;
import com.tjoeun.goldenstep.search.repository.SearchSessionRepository;

import jakarta.persistence.EntityManager;

@SpringBootTest
@Transactional
class TimeResultQueryTest {

    @Autowired
    private TimeResultService timeResultService;

    @Autowired
    private SearchSessionRepository searchSessionRepository;

    @Autowired
    private AnalysisRunRepository analysisRunRepository;

    @Autowired
    private EntityManager entityManager;

    private TestRun testRun;

    @BeforeEach
    void setUp() {
        testRun = createRun();
    }

    @Test
    void 저장된_현재_결과와_순위순_장소를_조회한다() {
        Long resultId = timeResultService.saveResult(
                testRun.runId,
                TimePoint.NOW,
                now(),
                Map.of("type", "Polygon"),
                "AVAILABLE",
                List.of(
                        place((byte) 2, "poi-2"),
                        place((byte) 1, "poi-1")
                )
        );

        entityManager.flush();
        entityManager.clear();

        TimeResultResponse response = timeResultService.getStoredResult(
                testRun.runId,
                "NOW",
                testRun.recoveryToken
        );

        assertEquals(testRun.runId, response.getRunId());
        assertEquals(resultId, response.getResultId());
        assertEquals(TimePoint.NOW, response.getTimePoint());
        assertEquals("AVAILABLE", response.getReliabilityStatus());
        assertNotNull(response.getBoundaryZone());

        assertEquals(2, response.getPlaces().size());
        assertEquals((byte) 1, response.getPlaces().get(0).getPriorityRank());
        assertEquals("poi-1", response.getPlaces().get(0).getPoiId());
        assertEquals((byte) 2, response.getPlaces().get(1).getPriorityRank());
        assertEquals("poi-2", response.getPlaces().get(1).getPoiId());
    }

    @Test
    void 저장되지_않은_시간점은_조회할_수_없다() {
        saveNow();

        RestException exception = assertThrows(
                RestException.class,
                () -> timeResultService.getStoredResult(
                        testRun.runId,
                        "AFTER_1H",
                        testRun.recoveryToken
                )
        );

        assertEquals(
                ErrorCode.TIME_RESULT_NOT_FOUND,
                exception.getErrorCode()
        );
    }

    @Test
    void 지원하지_않는_시간점은_거부한다() {
        RestException exception = assertThrows(
                RestException.class,
                () -> timeResultService.getStoredResult(
                        testRun.runId,
                        "AFTER_2H",
                        testRun.recoveryToken
                )
        );

        assertEquals(
                ErrorCode.INVALID_TIME_POINT,
                exception.getErrorCode()
        );
    }

    @Test
    void 다른_세션의_분석_결과는_조회할_수_없다() {
        saveNow();

        TestRun otherRun = createRun();

        RestException exception = assertThrows(
                RestException.class,
                () -> timeResultService.getStoredResult(
                        testRun.runId,
                        "NOW",
                        otherRun.recoveryToken
                )
        );

        assertEquals(
                ErrorCode.ANALYSIS_RUN_NOT_FOUND,
                exception.getErrorCode()
        );
    }

    private void saveNow() {
        timeResultService.saveResult(
                testRun.runId,
                TimePoint.NOW,
                now(),
                null,
                "AVAILABLE",
                List.of(place((byte) 1, "poi-now-1"))
        );

        entityManager.flush();
        entityManager.clear();
    }

    private TestRun createRun() {
        LocalDateTime requestedAt = now();

        String recoveryToken =
                "test-" + UUID.randomUUID();

        SearchSession session = searchSessionRepository.save(
                new SearchSession(
                        sha256(recoveryToken),
                        new BigDecimal("37.5665000"),
                        new BigDecimal("126.9780000"),
                        "서울특별시 중구 세종대로 110",
                        requestedAt.minusMinutes(5),
                        Map.of("personType", "CHILD"),
                        requestedAt
                )
        );

        AnalysisRun run = analysisRunRepository.save(
                new AnalysisRun(session, requestedAt)
        );

        return new TestRun(run.getId(), recoveryToken);
    }

    private TimeResultService.PriorityPlaceData place(
            byte rank,
            String poiId
    ) {
        return new TimeResultService.PriorityPlaceData(
                poiId,
                rank,
                "테스트 장소 " + rank,
                "서울특별시 중구",
                new BigDecimal("37.5666000"),
                new BigDecimal("126.9781000"),
                new BigDecimal("0.7500000")
        );
    }

    private LocalDateTime now() {
        return LocalDateTime.now(ZoneId.of("Asia/Seoul"))
                .truncatedTo(ChronoUnit.SECONDS);
    }

    private String sha256(String value) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "테스트용 토큰 해시를 생성할 수 없습니다.",
                    exception
            );
        }
    }

    private static class TestRun {

        private final Long runId;
        private final String recoveryToken;

        private TestRun(Long runId, String recoveryToken) {
            this.runId = runId;
            this.recoveryToken = recoveryToken;
        }
    }
}