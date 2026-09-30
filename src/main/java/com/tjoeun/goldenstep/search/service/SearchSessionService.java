package com.tjoeun.goldenstep.search.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tjoeun.goldenstep.analysis.entity.AnalysisRun;
import com.tjoeun.goldenstep.analysis.entity.Status;
import com.tjoeun.goldenstep.analysis.event.AnalysisRequestedEvent;
import com.tjoeun.goldenstep.analysis.repository.AnalysisRunRepository;
import com.tjoeun.goldenstep.global.exception.ErrorCode;
import com.tjoeun.goldenstep.global.exception.RestException;
import com.tjoeun.goldenstep.search.dto.request.ConditionStatus;
import com.tjoeun.goldenstep.search.dto.request.CreateSearchSessionRequest;
import com.tjoeun.goldenstep.search.entity.SearchSession;
import com.tjoeun.goldenstep.search.repository.SearchSessionRepository;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SearchSessionService {

	private static final ZoneId SERVICE_ZONE = ZoneId.of("Asia/Seoul");
	private static final SecureRandom SECURE_RANDOM = new SecureRandom();

	private final SearchSessionRepository searchSessionRepository;
	private final AnalysisRunRepository analysisRunRepository;
	private final ApplicationEventPublisher eventPublisher;

	@Transactional
	public CreateResult create(CreateSearchSessionRequest request) {
		LocalDateTime now = LocalDateTime.now(SERVICE_ZONE).truncatedTo(ChronoUnit.SECONDS);

		validateLastSeenAt(request.getLastSeenAt(), now);

		String recoveryToken = generateRecoveryToken();
		String recoveryTokenHash = sha256(recoveryToken);

        SearchSession session = new SearchSession(
                recoveryTokenHash,
                request.getLastLat(),
                request.getLastLng(),
                normalizeOptionalText(request.getLastAddress()),
                request.getLastSeenAt(),
                createPersonAttributes(request),
                now
        );

		SearchSession savedSession = searchSessionRepository.save(session);
		
		AnalysisRun run = new AnalysisRun(savedSession, now);
		
	    AnalysisRun savedRun = analysisRunRepository.save(run);
	    
	    eventPublisher.publishEvent(
	            new AnalysisRequestedEvent(savedRun.getId())
	    );

	    return new CreateResult(
	            savedSession.getId(),
	            savedRun.getId(),
	            savedRun.getStatus(),
	            recoveryToken,
	            savedSession.getExpiresAt()
	    );
	}

	private void validateLastSeenAt(LocalDateTime lastSeenAt, LocalDateTime now) {
		if (!lastSeenAt.isBefore(now)) {
			throw new RestException(ErrorCode.INVALID_LAST_SEEN_AT);
		}

		if (lastSeenAt.getMinute() % 5 != 0 || lastSeenAt.getSecond() != 0 || lastSeenAt.getNano() != 0) {
			throw new RestException(ErrorCode.INVALID_MINUTE_INTERVAL);
		}
	}

	private Map<String, Object> createPersonAttributes(CreateSearchSessionRequest request) {
		Map<String, Object> attributes = new HashMap<>();

		attributes.put("personType", request.getPersonType().name());
		attributes.put("disabilityStatus", normalizeCondition(request.getDisabilityStatus()).name());
		attributes.put("diseaseStatus", normalizeCondition(request.getDiseaseStatus()).name());

		Integer age = request.getAge();
		if (age != null) {
			attributes.put("age", age);
			attributes.put("ageQualifier", ageQualifier(age));
		}

		String physicalFeatures = normalizeOptionalText(request.getPhysicalFeatures());
		if (physicalFeatures != null) {
			attributes.put("physicalFeatures", physicalFeatures);
		}

		return attributes;
	}

	private ConditionStatus normalizeCondition(ConditionStatus status) {
		return status.normalized();
	}

	private String ageQualifier(int age) {
		if (age == CreateSearchSessionRequest.MIN_AGE_OPTION) {
			return "AT_MOST";
		}
		if (age == CreateSearchSessionRequest.MAX_AGE_OPTION) {
			return "AT_LEAST";
		}
		return "EXACT";
	}

	private String normalizeOptionalText(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return value.trim();
	}

	private String generateRecoveryToken() {
		byte[] randomBytes = new byte[32];
		SECURE_RANDOM.nextBytes(randomBytes);
		return HexFormat.of().formatHex(randomBytes);
	}

	private String sha256(String value) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(hash);
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256 해시 기능을 사용할 수 없습니다.", exception);
		}
	}

	@Getter
	public static class CreateResult {

	    private final Long sessionId;
	    private final Long runId;
	    private final Status status;
	    private final String recoveryToken;
	    private final LocalDateTime expiresAt;

	    public CreateResult(
	            Long sessionId,
	            Long runId,
	            Status status,
	            String recoveryToken,
	            LocalDateTime expiresAt
	    ) {
	        this.sessionId = sessionId;
	        this.runId = runId;
	        this.status = status;
	        this.recoveryToken = recoveryToken;
	        this.expiresAt = expiresAt;
	    }
	}
	
	public SearchSession restore(String recoveryToken) {
	    if (recoveryToken == null || recoveryToken.isBlank()) {
	        throw new RestException(ErrorCode.RECOVERY_TOKEN_MISSING);
	    }

	    String tokenHash = sha256(recoveryToken);

	    SearchSession session = searchSessionRepository
	            .findByRecoveryTokenHash(tokenHash)
	            .orElseThrow(() ->
	                    new RestException(ErrorCode.SEARCH_SESSION_NOT_FOUND)
	            );

	    LocalDateTime now = LocalDateTime.now(SERVICE_ZONE);

	    if (!session.getExpiresAt().isAfter(now)) {
	        throw new RestException(ErrorCode.SEARCH_SESSION_EXPIRED);
	    }

	    return session;
	}
}