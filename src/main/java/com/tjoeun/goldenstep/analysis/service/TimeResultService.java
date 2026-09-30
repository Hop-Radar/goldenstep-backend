package com.tjoeun.goldenstep.analysis.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tjoeun.goldenstep.ai.dto.request.AnalysisPredictionRequest;
import com.tjoeun.goldenstep.analysis.dto.response.TimeResultResponse;
import com.tjoeun.goldenstep.analysis.entity.AnalysisRun;
import com.tjoeun.goldenstep.analysis.entity.PriorityPlace;
import com.tjoeun.goldenstep.analysis.entity.Status;
import com.tjoeun.goldenstep.analysis.entity.TimePoint;
import com.tjoeun.goldenstep.analysis.entity.TimeResult;
import com.tjoeun.goldenstep.analysis.repository.AnalysisRunRepository;
import com.tjoeun.goldenstep.analysis.repository.PriorityPlaceRepository;
import com.tjoeun.goldenstep.analysis.repository.TimeResultRepository;
import com.tjoeun.goldenstep.global.exception.ErrorCode;
import com.tjoeun.goldenstep.global.exception.RestException;
import com.tjoeun.goldenstep.search.entity.SearchSession;
import com.tjoeun.goldenstep.search.service.SearchSessionService;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TimeResultService {

	private final AnalysisRunRepository analysisRunRepository;
	private final TimeResultRepository timeResultRepository;
	private final PriorityPlaceRepository priorityPlaceRepository;
	
	private final SearchSessionService searchSessionService;

	@Transactional
	public Long saveResult(Long runId, TimePoint timePoint, LocalDateTime targetAt, Map<String, Object> boundaryZone,
			String reliabilityStatus, List<PriorityPlaceData> places) {
		if (runId == null || timePoint == null || targetAt == null || places == null) {
			throw new IllegalArgumentException("분석 실행 ID, 시간점, 대상 시각, 장소 목록은 필수입니다.");
		}

		if (places.size() > 3) {
			throw new IllegalArgumentException("한 시간점의 추천 장소는 최대 3개까지 저장할 수 있습니다.");
		}

		validatePlaces(places);

		AnalysisRun run = analysisRunRepository.findByIdForUpdate(runId)
				.orElseThrow(() -> new RestException(ErrorCode.ANALYSIS_RUN_NOT_FOUND));

		if (timePoint == TimePoint.NOW && run.getStatus() != Status.PROCESSING) {
			throw new IllegalStateException("현재 시간점 결과는 진행 중인 분석에만 저장할 수 있습니다.");
		}

		if (timePoint != TimePoint.NOW && run.getStatus() != Status.COMPLETED) {
			throw new IllegalStateException("다른 시간점은 현재 시간점 분석이 완료된 후 요청할 수 있습니다.");
		}

		if (timeResultRepository.existsByAnalysisRunAndTimePoint(run, timePoint)) {
			throw new IllegalStateException("이 분석 실행의 해당 시간점 결과는 이미 저장되었습니다.");
		}

		TimeResult timeResult = new TimeResult(run, timePoint, targetAt, boundaryZone, reliabilityStatus);

		TimeResult savedResult = timeResultRepository.save(timeResult);

		List<PriorityPlace> priorityPlaces = new ArrayList<>();

		for (PriorityPlaceData place : places) {
			priorityPlaces.add(new PriorityPlace(savedResult, place.getPoiId(), place.getPriorityRank(),
					place.getName(), place.getAddress(), place.getLat(), place.getLng(), place.getScore()));
		}

		priorityPlaceRepository.saveAll(priorityPlaces);

		if (timePoint == TimePoint.NOW) {
		    LocalDateTime completedAt = LocalDateTime.now(
		            ZoneId.of("Asia/Seoul")
		    ).truncatedTo(ChronoUnit.SECONDS);

		    run.complete(completedAt);
		}

		return savedResult.getId();
	}

	private void validatePlaces(List<PriorityPlaceData> places) {
		Set<Byte> ranks = new HashSet<>();
		Set<String> poiIds = new HashSet<>();

		for (PriorityPlaceData place : places) {
			if (place == null || place.getPoiId() == null || place.getPoiId().isBlank()
					|| place.getPriorityRank() == null || place.getName() == null || place.getName().isBlank()
					|| place.getLat() == null || place.getLng() == null) {
				throw new IllegalArgumentException("추천 장소의 식별자, 순위, 이름, 위도, 경도는 필수입니다.");
			}

			int rank = place.getPriorityRank();

			if (rank < 1 || rank > 3) {
				throw new IllegalArgumentException("추천 장소 순위는 1~3이어야 합니다.");
			}

			if (!ranks.add(place.getPriorityRank())) {
				throw new IllegalArgumentException("같은 시간점에 중복된 추천 순위가 있습니다.");
			}

			if (!poiIds.add(place.getPoiId())) {
				throw new IllegalArgumentException("같은 시간점에 중복된 장소 식별자가 있습니다.");
			}

			if (place.getLat().compareTo(BigDecimal.valueOf(-90)) < 0
					|| place.getLat().compareTo(BigDecimal.valueOf(90)) > 0
					|| place.getLng().compareTo(BigDecimal.valueOf(-180)) < 0
					|| place.getLng().compareTo(BigDecimal.valueOf(180)) > 0) {
				throw new IllegalArgumentException("추천 장소의 위도 또는 경도가 유효 범위를 벗어났습니다.");
			}
		}
	}

	@Transactional(readOnly = true)
	public Optional<Long> findStoredResultId(
	        Long runId,
	        TimePoint timePoint
	) {
	    AnalysisRun run = analysisRunRepository.findById(runId)
				.orElseThrow(() -> new RestException(ErrorCode.ANALYSIS_RUN_NOT_FOUND));

	    return timeResultRepository
	            .findByAnalysisRunAndTimePoint(run, timePoint)
	            .map(TimeResult::getId);
	}

	@Transactional(readOnly = true)
	public TimeResultResponse getStoredResult(Long runId, String timePointValue, String recoveryToken) {
	    TimePoint timePoint;

	    try {
	        timePoint = TimePoint.valueOf(timePointValue);
	    } catch (IllegalArgumentException exception) {
	        throw new RestException(ErrorCode.INVALID_TIME_POINT);
	    }

		SearchSession session = searchSessionService.restore(recoveryToken);

	    AnalysisRun run = analysisRunRepository
	            .findByIdAndSearchSession(runId, session)
				.orElseThrow(() -> new RestException(ErrorCode.ANALYSIS_RUN_NOT_FOUND));

	    TimeResult result = timeResultRepository
	            .findByAnalysisRunAndTimePoint(run, timePoint)
				.orElseThrow(() -> new RestException(ErrorCode.TIME_RESULT_NOT_FOUND));

		List<PriorityPlace> places = priorityPlaceRepository.findByTimeResultOrderByPriorityRankAsc(result);

	    return new TimeResultResponse(result, places);
	}
	
	@Transactional(readOnly = true)
	public AnalysisPredictionRequest createTimePointPredictionRequest(
	        String recoveryToken,
	        Long runId,
	        TimePoint timePoint
	) {
	    if (runId == null || timePoint == null) {
	        throw new RestException(ErrorCode.INVALID_INPUT);
	    }

		SearchSession session = searchSessionService.restore(recoveryToken);

	    AnalysisRun run = analysisRunRepository
	            .findByIdAndSearchSession(runId, session)
				.orElseThrow(() -> new RestException(ErrorCode.ANALYSIS_RUN_NOT_FOUND));

	    if (run.getStatus() != Status.COMPLETED) {
			throw new RestException(ErrorCode.ANALYSIS_NOT_COMPLETED);
	    }

	    return new AnalysisPredictionRequest(
	            timePoint,
	            timePoint.calculateTargetAt(run.getRequestedAt()),
	            session.getLastLat(),
	            session.getLastLng(),
	            session.getLastSeenAt(),
	            new HashMap<>(session.getPersonAttributes())
	    );
	}

	@Getter
	public static class PriorityPlaceData {
		
		private final String poiId;
		private final Byte priorityRank;
		private final String name;
		private final String address;
		private final BigDecimal lat;
		private final BigDecimal lng;
		private final BigDecimal score;
		
		public PriorityPlaceData(String poiId, Byte priorityRank, String name, String address, BigDecimal lat,
				BigDecimal lng, BigDecimal score) {
			this.poiId = poiId;
			this.priorityRank = priorityRank;
			this.name = name;
			this.address = address;
			this.lat = lat;
			this.lng = lng;
			this.score = score;
		}
	}
}