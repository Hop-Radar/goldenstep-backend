package com.tjoeun.goldenstep.analysis.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.tjoeun.goldenstep.analysis.entity.AnalysisRun;
import com.tjoeun.goldenstep.analysis.entity.Status;
import com.tjoeun.goldenstep.analysis.entity.TimePoint;
import com.tjoeun.goldenstep.analysis.entity.TimeResult;
import com.tjoeun.goldenstep.analysis.repository.AnalysisRunRepository;
import com.tjoeun.goldenstep.analysis.repository.PriorityPlaceRepository;
import com.tjoeun.goldenstep.analysis.repository.TimeResultRepository;
import com.tjoeun.goldenstep.search.entity.SearchSession;
import com.tjoeun.goldenstep.search.repository.SearchSessionRepository;

import jakarta.persistence.EntityManager;

@SpringBootTest
@Transactional
class TimeResultServiceTest {

    @Autowired
    private TimeResultService timeResultService;

    @Autowired
    private SearchSessionRepository searchSessionRepository;

    @Autowired
    private AnalysisRunRepository analysisRunRepository;

    @Autowired
    private TimeResultRepository timeResultRepository;

    @Autowired
    private PriorityPlaceRepository priorityPlaceRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void 현재_결과를_저장하면_분석이_완료되고_이후_시간점도_저장된다() {
        AnalysisRun run = createRun();

        long resultCountBefore = timeResultRepository.count();
        long placeCountBefore = priorityPlaceRepository.count();

        Long nowResultId = timeResultService.saveResult(
                run.getId(),
                TimePoint.NOW,
                LocalDateTime.now(),
                null,
                "AVAILABLE",
                List.of(place((byte) 1, "poi-now-1"))
        );

        entityManager.flush();
        entityManager.clear();

        AnalysisRun completedRun = analysisRunRepository
                .findById(run.getId())
                .orElseThrow();

        assertEquals(Status.COMPLETED, completedRun.getStatus());
        assertNotNull(completedRun.getCompletedAt());

        LocalDateTime completedAt = completedRun.getCompletedAt();

        TimeResult nowResult = timeResultRepository
                .findById(nowResultId)
                .orElseThrow();

        assertEquals(TimePoint.NOW, nowResult.getTimePoint());

        Long oneHourResultId = timeResultService.saveResult(
                run.getId(),
                TimePoint.AFTER_1H,
                LocalDateTime.now().plusHours(1),
                null,
                "AVAILABLE",
                List.of(place((byte) 1, "poi-one-hour-1"))
        );

        entityManager.flush();
        entityManager.clear();

        assertEquals(
                TimePoint.AFTER_1H,
                timeResultRepository.findById(oneHourResultId)
                        .orElseThrow()
                        .getTimePoint()
        );

        AnalysisRun runAfterOneHour = analysisRunRepository
                .findById(run.getId())
                .orElseThrow();

        assertEquals(Status.COMPLETED, runAfterOneHour.getStatus());
        assertEquals(completedAt, runAfterOneHour.getCompletedAt());
        assertEquals(resultCountBefore + 2, timeResultRepository.count());
        assertEquals(placeCountBefore + 2, priorityPlaceRepository.count());
    }

    @Test
    void 같은_시간점_결과를_두_번_저장할_수_없다() {
        AnalysisRun run = createRun();

        timeResultService.saveResult(
                run.getId(),
                TimePoint.NOW,
                LocalDateTime.now(),
                null,
                "AVAILABLE",
                List.of(place((byte) 1, "poi-now-1"))
        );

        assertThrows(
                IllegalStateException.class,
                () -> timeResultService.saveResult(
                        run.getId(),
                        TimePoint.NOW,
                        LocalDateTime.now(),
                        null,
                        "AVAILABLE",
                        List.of(place((byte) 1, "poi-now-2"))
                )
        );
    }

    private AnalysisRun createRun() {
        LocalDateTime now = LocalDateTime
                .now(ZoneId.of("Asia/Seoul"))
                .truncatedTo(ChronoUnit.SECONDS);

        String tokenHash = UUID.randomUUID()
                .toString()
                .replace("-", "")
                .repeat(2);

        SearchSession session = searchSessionRepository.save(
                new SearchSession(
                        tokenHash,
                        new BigDecimal("37.5665000"),
                        new BigDecimal("126.9780000"),
                        "서울특별시 중구 세종대로 110",
                        now.minusMinutes(5),
                        Map.of("personType", "CHILD"),
                        now
                )
        );

        return analysisRunRepository.save(
                new AnalysisRun(session, now)
        );
    }

    private TimeResultService.PriorityPlaceData place(
            byte rank,
            String poiId
    ) {
        return new TimeResultService.PriorityPlaceData(
                poiId,
                rank,
                "테스트 장소",
                "서울특별시 중구",
                new BigDecimal("37.5666000"),
                new BigDecimal("126.9781000"),
                new BigDecimal("0.7500000")
        );
    }
}