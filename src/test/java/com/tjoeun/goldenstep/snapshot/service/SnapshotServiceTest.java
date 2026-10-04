package com.tjoeun.goldenstep.snapshot.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import com.tjoeun.goldenstep.analysis.dto.response.PriorityPlaceResponse;
import com.tjoeun.goldenstep.analysis.dto.response.TimeResultResponse;
import com.tjoeun.goldenstep.analysis.entity.TimePoint;
import com.tjoeun.goldenstep.analysis.service.TimeResultService;
import com.tjoeun.goldenstep.global.exception.ErrorCode;
import com.tjoeun.goldenstep.global.exception.RestException;
import com.tjoeun.goldenstep.search.entity.SearchSession;
import com.tjoeun.goldenstep.search.service.SearchSessionService;
import com.tjoeun.goldenstep.snapshot.dto.request.CreateSnapshotRequest;
import com.tjoeun.goldenstep.snapshot.entity.Snapshot;
import com.tjoeun.goldenstep.snapshot.entity.SnapshotPlace;
import com.tjoeun.goldenstep.snapshot.repository.SnapshotPlaceRepository;
import com.tjoeun.goldenstep.snapshot.repository.SnapshotRepository;

class SnapshotServiceTest {
    private final SnapshotRepository snapshots = mock(SnapshotRepository.class);
    private final SnapshotPlaceRepository places = mock(SnapshotPlaceRepository.class);
    private final SearchSessionService sessions = mock(SearchSessionService.class);
    private final TimeResultService results = mock(TimeResultService.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-04T03:00:00Z"), ZoneId.of("Asia/Seoul"));
    private final LocalDateTime now = LocalDateTime.now(clock);
    private final String token = "a".repeat(43);
    private SnapshotService service;
    private SearchSession session;

    @BeforeEach
    void setUp() {
        service = new SnapshotService(snapshots, places, sessions, results, "https://example.com/", clock);
        session = new SearchSession("owner-hash", new BigDecimal("37.5"), new BigDecimal("127.0"),
                "서울", now.minusHours(1), new HashMap<>(Map.of("physicalFeatures", "파란 옷")), now);
        when(sessions.restore("owner-cookie")).thenReturn(session);
        when(snapshots.save(any())).thenAnswer(invocation -> {
            Snapshot saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 7L);
            return saved;
        });
    }

    @Test
    void 발급시_토큰은_해시로_저장하고_공유당시_데이터를_복사한다() {
        TimeResultResponse result = mock(TimeResultResponse.class);
        List<Double> coordinates = new ArrayList<>(List.of(127.0, 37.5));
        Map<String, Object> boundary = new HashMap<>(Map.of("type", "Polygon", "coordinates", coordinates));
        PriorityPlaceResponse place = mock(PriorityPlaceResponse.class);
        when(place.getPriorityRank()).thenReturn((byte) 1);
        when(place.getName()).thenReturn("장소");
        when(place.getLat()).thenReturn(BigDecimal.valueOf(37.5));
        when(place.getLng()).thenReturn(BigDecimal.valueOf(127));
        when(place.getCheckedAt()).thenReturn(now.minusMinutes(1));
        when(result.getBoundaryZone()).thenReturn(boundary);
        when(result.getPlaces()).thenReturn(List.of(place));
        when(results.getStoredResult(1L, "AFTER_1H", "owner-cookie")).thenReturn(result);

        var response = service.create(new CreateSnapshotRequest(1L, TimePoint.AFTER_1H), "owner-cookie");
        assertEquals(now.plusHours(12), response.getExpiresAt());
        assertEquals(now, response.getCreatedAt());
        assertTrue(response.getShareUrl().startsWith("https://example.com/share-map.html#token="));
        String issuedToken = response.getShareUrl().split("token=")[1];
        assertTrue(issuedToken.matches("[A-Za-z0-9_-]{43}"));
        ArgumentCaptor<Snapshot> snapshotCaptor = ArgumentCaptor.forClass(Snapshot.class);
        verify(snapshots).save(snapshotCaptor.capture());
        Snapshot saved = snapshotCaptor.getValue();
        assertEquals(SnapshotService.hash(issuedToken), saved.getTokenHash());
        assertNotEquals(issuedToken, saved.getTokenHash());
        coordinates.set(0, 0.0);
        session.getPersonAttributes().put("physicalFeatures", "수정됨");
        assertEquals(List.of(127.0, 37.5), saved.getBoundaryZone().get("coordinates"));
        assertEquals("파란 옷", saved.getAdditionalInfo());
        assertEquals(session.getLastSeenAt(), saved.getLastSeenAt());
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<SnapshotPlace>> placeCaptor = ArgumentCaptor.forClass(List.class);
        verify(places).saveAll(placeCaptor.capture());
        assertEquals(now.minusMinutes(1), placeCaptor.getValue().getFirst().getCheckedAt());
    }

    @Test
    void 타인_분석이나_미완료_결과는_스냅샷을_생성하지_않는다() {
        when(results.getStoredResult(99L, "NOW", "owner-cookie"))
                .thenThrow(new RestException(ErrorCode.ANALYSIS_RUN_NOT_FOUND));
        assertThrows(RestException.class,
                () -> service.create(new CreateSnapshotRequest(99L, TimePoint.NOW), "owner-cookie"));
        verifyNoInteractions(snapshots, places);
    }

    @Test
    void 공유_조회는_세션쿠키없이_가능하고_원본세션에_의존하지_않는다() {
        Snapshot snapshot = new Snapshot(session, SnapshotService.hash(token), TimePoint.AFTER_1H, null, now);
        ReflectionTestUtils.setField(snapshot, "searchSession", null);
        when(snapshots.findByTokenHash(SnapshotService.hash(token))).thenReturn(Optional.of(snapshot));
        when(places.findBySnapshotOrderByPriorityRankAsc(snapshot)).thenReturn(List.of());
        var response = service.get(token);
        assertEquals("서울", response.getAddress());
        assertEquals(60, response.getSelectedMinutes());
        assertEquals("파란 옷", response.getAdditionalInfo());
        verifyNoInteractions(sessions, results);
    }

    @Test
    void 만료_직전은_가능하지만_정확히_12시간에는_410이다() {
        Snapshot snapshot = new Snapshot(session, SnapshotService.hash(token), TimePoint.NOW, null, now.minusHours(12));
        assertFalse(snapshot.isUnavailable(now.minusSeconds(1)));
        when(snapshots.findByTokenHash(SnapshotService.hash(token))).thenReturn(Optional.of(snapshot));
        assertEquals(ErrorCode.SNAPSHOT_UNAVAILABLE, assertThrows(RestException.class,
                () -> service.get(token)).getErrorCode());
        verifyNoInteractions(places);
    }

    @Test
    void 폐기된_링크는_만료전에도_410이다() {
        Snapshot snapshot = new Snapshot(session, SnapshotService.hash(token), TimePoint.NOW, null, now);
        snapshot.revoke(now);
        when(snapshots.findByTokenHash(SnapshotService.hash(token))).thenReturn(Optional.of(snapshot));
        assertEquals(ErrorCode.SNAPSHOT_UNAVAILABLE, assertThrows(RestException.class,
                () -> service.get(token)).getErrorCode());
    }

    @Test
    void 없는_토큰과_잘못된_형식은_404이다() {
        when(snapshots.findByTokenHash(anyString())).thenReturn(Optional.empty());
        assertEquals(ErrorCode.SNAPSHOT_NOT_FOUND, assertThrows(RestException.class,
                () -> service.get(token)).getErrorCode());
        clearInvocations(snapshots);
        assertEquals(ErrorCode.SNAPSHOT_NOT_FOUND, assertThrows(RestException.class,
                () -> service.get("invalid")).getErrorCode());
        verifyNoInteractions(snapshots);
    }

    @Test
    void 발급자만_폐기할수있고_반복폐기는_최초시각을_유지한다() {
        Snapshot snapshot = new Snapshot(session, SnapshotService.hash(token), TimePoint.NOW, null, now);
        when(snapshots.findByIdAndSearchSession(7L, session)).thenReturn(Optional.of(snapshot));
        service.revoke(7L, "owner-cookie");
        snapshot.revoke(now.plusHours(1));
        service.revoke(7L, "owner-cookie");
        assertEquals(now, snapshot.getRevokedAt());
        when(snapshots.findByIdAndSearchSession(8L, session)).thenReturn(Optional.empty());
        assertEquals(ErrorCode.SNAPSHOT_NOT_FOUND, assertThrows(RestException.class,
                () -> service.revoke(8L, "owner-cookie")).getErrorCode());
    }

    @Test
    void 쿠키가_없으면_발급과_폐기가_거부된다() {
        when(sessions.restore(null)).thenThrow(new RestException(ErrorCode.RECOVERY_TOKEN_MISSING));
        assertThrows(RestException.class, () -> service.create(new CreateSnapshotRequest(1L, TimePoint.NOW), null));
        assertThrows(RestException.class, () -> service.revoke(7L, null));
        verifyNoInteractions(snapshots, places, results);
    }
}
