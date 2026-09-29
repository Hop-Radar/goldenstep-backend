package com.tjoeun.goldenstep.analysis.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tjoeun.goldenstep.analysis.dto.response.AnalysisStatusResponse;
import com.tjoeun.goldenstep.analysis.entity.AnalysisRun;
import com.tjoeun.goldenstep.analysis.repository.AnalysisRunRepository;
import com.tjoeun.goldenstep.global.exception.ErrorCode;
import com.tjoeun.goldenstep.global.exception.RestException;
import com.tjoeun.goldenstep.search.entity.SearchSession;
import com.tjoeun.goldenstep.search.service.SearchSessionService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AnalysisRunService {

	private final SearchSessionService searchSessionService;
	private final AnalysisRunRepository analysisRunRepository;

	@Transactional(readOnly = true)
	public AnalysisStatusResponse getStatus(Long runId, String recoveryToken) {
		SearchSession session = searchSessionService.restore(recoveryToken);

		AnalysisRun run = analysisRunRepository.findByIdAndSearchSession(runId, session)
				.orElseThrow(() -> new RestException(ErrorCode.ANALYSIS_RUN_NOT_FOUND));

		return new AnalysisStatusResponse(run);
	}
}