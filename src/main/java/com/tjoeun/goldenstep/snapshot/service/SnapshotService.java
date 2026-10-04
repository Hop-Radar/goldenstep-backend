package com.tjoeun.goldenstep.snapshot.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tjoeun.goldenstep.analysis.dto.response.TimeResultResponse;
import com.tjoeun.goldenstep.analysis.service.TimeResultService;
import com.tjoeun.goldenstep.global.exception.ErrorCode;
import com.tjoeun.goldenstep.global.exception.RestException;
import com.tjoeun.goldenstep.search.entity.SearchSession;
import com.tjoeun.goldenstep.search.service.SearchSessionService;
import com.tjoeun.goldenstep.snapshot.dto.request.CreateSnapshotRequest;
import com.tjoeun.goldenstep.snapshot.dto.response.CreateSnapshotResponse;
import com.tjoeun.goldenstep.snapshot.dto.response.SnapshotResponse;
import com.tjoeun.goldenstep.snapshot.entity.Snapshot;
import com.tjoeun.goldenstep.snapshot.entity.SnapshotPlace;
import com.tjoeun.goldenstep.snapshot.repository.SnapshotPlaceRepository;
import com.tjoeun.goldenstep.snapshot.repository.SnapshotRepository;

@Service
public class SnapshotService {

    private static final ZoneId SERVICE_ZONE = ZoneId.of("Asia/Seoul");
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final SnapshotRepository snapshotRepository;
    private final SnapshotPlaceRepository snapshotPlaceRepository;
    private final SearchSessionService searchSessionService;
    private final TimeResultService timeResultService;
    private final String frontendBaseUrl;
    private final Clock clock;

    @Autowired
    public SnapshotService(
            SnapshotRepository snapshotRepository,
            SnapshotPlaceRepository snapshotPlaceRepository,
            SearchSessionService searchSessionService,
            TimeResultService timeResultService,
            @Value("${goldenstep.snapshot.frontend-base-url}") String frontendBaseUrl
    ) {
        this(
                snapshotRepository, snapshotPlaceRepository, searchSessionService,
                timeResultService, frontendBaseUrl, Clock.system(SERVICE_ZONE)
        );
    }

    SnapshotService(
            SnapshotRepository snapshotRepository,
            SnapshotPlaceRepository snapshotPlaceRepository,
            SearchSessionService searchSessionService,
            TimeResultService timeResultService,
            String frontendBaseUrl,
            Clock clock
    ) {
        this.snapshotRepository = snapshotRepository;
        this.snapshotPlaceRepository = snapshotPlaceRepository;
        this.searchSessionService = searchSessionService;
        this.timeResultService = timeResultService;
        this.frontendBaseUrl = frontendBaseUrl.replaceAll("/+$", "");
        this.clock = clock;
    }

    @Transactional
    public CreateSnapshotResponse create(CreateSnapshotRequest request, String recoveryToken) {
        SearchSession session = searchSessionService.restore(recoveryToken);

        // 기존 결과 조회에서 분석 실행이 현재 탐색 세션에 속하는지 확인한다.
        TimeResultResponse result = timeResultService.getStoredResult(
                request.getRunId(), request.getTimePoint().name(), recoveryToken
        );

        String token = generateShareToken();
        Map<String, Object> boundary = copyJson(result.getBoundaryZone());
        Snapshot snapshot = snapshotRepository.save(
                new Snapshot(session, hash(token), request.getTimePoint(), boundary, now())
        );

        List<SnapshotPlace> snapshotPlaces = result.getPlaces().stream()
                .map(place -> new SnapshotPlace(snapshot, place))
                .toList();

        snapshotPlaceRepository.saveAll(snapshotPlaces);

        return new CreateSnapshotResponse(
                snapshot.getId(), frontendBaseUrl + "/share-map.html#token=" + token,
                snapshot.getCreatedAt(), snapshot.getExpiresAt()
        );
    }

    @Transactional(readOnly = true)
    public SnapshotResponse get(String token) {
        if (token == null || !token.matches("[A-Za-z0-9_-]{43}")) {
            throw new RestException(ErrorCode.SNAPSHOT_NOT_FOUND);
        }

        Snapshot snapshot = snapshotRepository.findByTokenHash(hash(token))
                .orElseThrow(() -> new RestException(ErrorCode.SNAPSHOT_NOT_FOUND));
        if (snapshot.isUnavailable(now())) {
            throw new RestException(ErrorCode.SNAPSHOT_UNAVAILABLE);
        }

        List<SnapshotPlace> snapshotPlaces = snapshotPlaceRepository.findBySnapshotOrderByPriorityRankAsc(snapshot);

        return new SnapshotResponse(snapshot, snapshotPlaces);
    }

    @Transactional
    public void revoke(Long snapshotId, String recoveryToken) {
        SearchSession session = searchSessionService.restore(recoveryToken);
        Snapshot snapshot = snapshotRepository.findByIdAndSearchSession(snapshotId, session)
                .orElseThrow(() -> new RestException(ErrorCode.SNAPSHOT_NOT_FOUND));
        snapshot.revoke(now());
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS);
    }

    private String generateShareToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);

        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String hash(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    // 중첩된 GeoJSON 컬렉션을 복사해 원본 분석 결과와 분리한다.
    @SuppressWarnings("unchecked")
    private static <T> T copyJson(T value) {
        if (value instanceof Map<?, ?> map) {
            Map<Object, Object> copy = new LinkedHashMap<>();
            map.forEach((key, item) -> copy.put(key, copyJson(item)));
            return (T) copy;
        }
        if (value instanceof List<?> list) {
            return (T) list.stream().map(SnapshotService::copyJson).toList();
        }
        return value;
    }
}
