package com.tjoeun.goldenstep.snapshot.service;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import com.tjoeun.goldenstep.analysis.entity.AnalysisRun;
import com.tjoeun.goldenstep.analysis.entity.TimePoint;
import com.tjoeun.goldenstep.analysis.repository.AnalysisRunRepository;
import com.tjoeun.goldenstep.analysis.service.PlaceCheckService;
import com.tjoeun.goldenstep.analysis.service.TimeResultService;
import com.tjoeun.goldenstep.global.exception.ErrorCode;
import com.tjoeun.goldenstep.global.exception.RestException;
import com.tjoeun.goldenstep.search.entity.SearchSession;
import com.tjoeun.goldenstep.search.repository.SearchSessionRepository;
import com.tjoeun.goldenstep.snapshot.dto.request.CreateSnapshotRequest;
import com.tjoeun.goldenstep.snapshot.dto.response.CreateSnapshotResponse;
import jakarta.persistence.EntityManager;

@SpringBootTest
@Transactional
class SnapshotPersistenceTest {
    @Autowired private SnapshotService service;
    @Autowired private SearchSessionRepository sessions;
    @Autowired private AnalysisRunRepository runs;
    @Autowired private TimeResultService results;
    @Autowired private PlaceCheckService checks;
    @Autowired private EntityManager entityManager;
    private String ownerToken;
    private Long sessionId;
    private Long runId;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        now = LocalDateTime.now(ZoneId.of("Asia/Seoul")).truncatedTo(ChronoUnit.SECONDS);
        ownerToken = UUID.randomUUID().toString();
        SearchSession session = sessions.save(new SearchSession(SnapshotService.hash(ownerToken),
                BigDecimal.valueOf(37.5), BigDecimal.valueOf(127), "서울", now.minusHours(1),
                Map.of("physicalFeatures", "파란 옷"), now));
        sessionId = session.getId();
        runId = runs.save(new AnalysisRun(session, now)).getId();
        results.saveResult(runId, TimePoint.NOW, now, Map.of("type", "Polygon", "coordinates",
                List.of(List.of(List.of(127.0, 37.5), List.of(127.1, 37.5), List.of(127.0, 37.6), List.of(127.0, 37.5)))),
                "AVAILABLE", List.of(new TimeResultService.PriorityPlaceData("poi-1", (byte) 1,
                        "장".repeat(200), "장소 주소", BigDecimal.valueOf(37.5), BigDecimal.valueOf(127), null)));
    }

    private CreateSnapshotResponse create() {
        return service.create(new CreateSnapshotRequest(runId, TimePoint.NOW), ownerToken);
    }

    private String token(CreateSnapshotResponse response) {
        return response.getShareUrl().split("token=")[1];
    }

    @Test
    void JSON과_긴장소명과_완료상태를_저장하고_원본변경후에도_유지한다() {
        Long placeId = results.getStoredResult(runId, "NOW", ownerToken).getPlaces().getFirst().getPlaceId();
        CreateSnapshotResponse beforeCheck = create();
        checks.check(placeId, ownerToken);
        CreateSnapshotResponse afterCheck = create();
        entityManager.flush();
        entityManager.clear();
        var before = service.get(token(beforeCheck));
        var after = service.get(token(afterCheck));
        assertFalse(before.getPriorityPlaces().getFirst().isChecked());
        assertTrue(after.getPriorityPlaces().getFirst().isChecked());
        assertNotNull(after.getPriorityPlaces().getFirst().getCheckedAt());
        assertEquals("장".repeat(200), after.getPriorityPlaces().getFirst().getName());
        assertEquals("Polygon", after.getBoundaryZone().get("type"));
        assertEquals("파란 옷", after.getAdditionalInfo());
        assertEquals(now.minusHours(1), after.getLastSeenTime());
        assertEquals(beforeCheck.getCreatedAt().plusHours(12), before.getExpiresAt());
    }

    @Test
    void 원본세션_삭제후에도_공유조회는_가능하다() {
        CreateSnapshotResponse created = create();
        entityManager.flush();
        entityManager.createNativeQuery("DELETE FROM search_session WHERE id = :id")
                .setParameter("id", sessionId).executeUpdate();
        entityManager.clear();
        assertEquals("서울", service.get(token(created)).getAddress());
        assertEquals(1, service.get(token(created)).getPriorityPlaces().size());
    }

    @Test
    void 타인_분석_발급과_타인_링크폐기는_거부한다() {
        String otherToken = UUID.randomUUID().toString();
        sessions.saveAndFlush(new SearchSession(SnapshotService.hash(otherToken),
                BigDecimal.valueOf(37.5), BigDecimal.valueOf(127), "다른 세션", now.minusHours(1), Map.of(), now));
        assertEquals(ErrorCode.ANALYSIS_RUN_NOT_FOUND, assertThrows(RestException.class,
                () -> service.create(new CreateSnapshotRequest(runId, TimePoint.NOW), otherToken)).getErrorCode());
        CreateSnapshotResponse created = create();
        assertEquals(ErrorCode.SNAPSHOT_NOT_FOUND, assertThrows(RestException.class,
                () -> service.revoke(created.getSnapshotId(), otherToken)).getErrorCode());
    }

    @Test
    void 폐기시각은_DB에_저장되어_다시조회해도_410이다() {
        CreateSnapshotResponse created = create();
        service.revoke(created.getSnapshotId(), ownerToken);
        entityManager.flush();
        entityManager.clear();
        assertEquals(ErrorCode.SNAPSHOT_UNAVAILABLE, assertThrows(RestException.class,
                () -> service.get(token(created))).getErrorCode());
    }
}
