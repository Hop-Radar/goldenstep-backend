package com.tjoeun.goldenstep.analysis.service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.tjoeun.goldenstep.ai.client.AnalysisClient;
import com.tjoeun.goldenstep.ai.dto.request.AnalysisPredictionRequest;
import com.tjoeun.goldenstep.ai.dto.response.AnalysisPredictionResponse;
import com.tjoeun.goldenstep.ai.dto.response.AnalysisPredictionResponse.PriorityPoint;
import com.tjoeun.goldenstep.analysis.dto.response.AnalysisStatusResponse;
import com.tjoeun.goldenstep.analysis.entity.AnalysisRun;
import com.tjoeun.goldenstep.analysis.entity.Status;
import com.tjoeun.goldenstep.analysis.entity.TimePoint;
import com.tjoeun.goldenstep.analysis.repository.AnalysisRunRepository;
import com.tjoeun.goldenstep.analysis.service.TimeResultService.PriorityPlaceData;
import com.tjoeun.goldenstep.global.exception.ErrorCode;
import com.tjoeun.goldenstep.global.exception.RestException;
import com.tjoeun.goldenstep.location.service.ReverseGeocodingService;
import com.tjoeun.goldenstep.search.entity.SearchSession;
import com.tjoeun.goldenstep.search.service.SearchSessionService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AnalysisRunService {

	private final AnalysisRunRepository analysisRunRepository;
	private final AnalysisClient analysisClient;
	
	private final SearchSessionService searchSessionService;
	private final ReverseGeocodingService reverseGeocodingService;
	private final TimeResultService timeResultService;

	@Transactional(readOnly = true)
	public AnalysisStatusResponse getStatus(Long runId, String recoveryToken) {
		SearchSession session = searchSessionService.restore(recoveryToken);

		AnalysisRun run = analysisRunRepository.findByIdAndSearchSession(runId, session)
				.orElseThrow(() -> new RestException(ErrorCode.ANALYSIS_RUN_NOT_FOUND));

		return new AnalysisStatusResponse(run);
	}
	
	@Transactional(readOnly = true)
	public AnalysisPredictionRequest createInitialPredictionRequest(Long runId) {
	    AnalysisRun run = analysisRunRepository.findById(runId)
				.orElseThrow(() -> new RestException(ErrorCode.ANALYSIS_RUN_NOT_FOUND));

	    if (run.getStatus() != Status.PROCESSING) {
			throw new RestException(ErrorCode.INVALID_INITIAL_ANALYSIS_STATUS);
	    }

	    SearchSession session = run.getSearchSession();

	    return new AnalysisPredictionRequest(
	            TimePoint.NOW,
	            run.getRequestedAt(),
	            session.getLastLat(),
	            session.getLastLng(),
	            session.getLastSeenAt(),
	            new HashMap<>(session.getPersonAttributes())
	    );
	}
	
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public void executeInitialAnalysis(Long runId, AnalysisPredictionRequest request) {
	    if (request == null) {
			throw new RestException(ErrorCode.ANALYSIS_REQUEST_MISSING);
	    }

		AnalysisPredictionResponse prediction = analysisClient.analyze(request);

	    if (prediction == null
	            || !"SUCCESS".equals(prediction.getStatus())
	            || prediction.getSummary() == null
	            || prediction.getPriorityPoints() == null) {

			throw new RestException(ErrorCode.INVALID_ANALYSIS_RESPONSE);
	    }

	    List<PriorityPlaceData> places = new ArrayList<>();

	    for (PriorityPoint point : prediction.getPriorityPoints()) {
	        if (point == null
	                || point.getLocation() == null
	                || point.getLocation().getLat() == null
	                || point.getLocation().getLon() == null) {

				throw new RestException(ErrorCode.INVALID_ANALYSIS_RESPONSE);
	        }

	        String address = reverseGeocodingService.findAddress(
	                point.getLocation().getLat(),
	                point.getLocation().getLon()
	        );

	        places.add(new PriorityPlaceData(
	                point.getPoiId(),
	                point.getRank(),
	                point.getName(),
	                address,
	                point.getLocation().getLat(),
	                point.getLocation().getLon(),
	                point.getScore()
	        ));
	    }

	    timeResultService.saveResult(
	            runId,
	            TimePoint.NOW,
	            request.getTargetAt(),
	            prediction.getBoundaryZone(),
	            prediction.getSummary().getReliabilityStatus(),
	            places
	    );
	}
	
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void markInitialAnalysisFailed(Long runId) {
	    AnalysisRun run = analysisRunRepository
	            .findByIdForUpdate(runId)
	            .orElseThrow(() -> new RestException(
	                    ErrorCode.ANALYSIS_RUN_NOT_FOUND
	            ));

	    if (run.getStatus() != Status.PROCESSING) {
	        return;
	    }

	    LocalDateTime failedAt = LocalDateTime
	            .now(ZoneId.of("Asia/Seoul"))
	            .truncatedTo(ChronoUnit.SECONDS);

	    if (failedAt.isBefore(run.getRequestedAt())) {
	        failedAt = run.getRequestedAt();
	    }

	    run.fail(failedAt);
	}
}