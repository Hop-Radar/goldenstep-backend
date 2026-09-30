package com.tjoeun.goldenstep.analysis.event;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.tjoeun.goldenstep.ai.dto.request.AnalysisPredictionRequest;
import com.tjoeun.goldenstep.analysis.service.AnalysisRunService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class AnalysisRequestedEventListener {

    private final AnalysisRunService analysisRunService;

    @Async("analysisExecutor")
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(AnalysisRequestedEvent event) {
        Long runId = event.getRunId();

        try {
            AnalysisPredictionRequest request =
					analysisRunService.createInitialPredictionRequest(runId);

            analysisRunService.executeInitialAnalysis(
                    runId,
                    request
            );

        } catch (Exception exception) {
            log.error(
                    "초기 분석 처리에 실패했습니다. runId={}",
                    runId,
                    exception
            );

            try {
				analysisRunService.markInitialAnalysisFailed(runId);

            } catch (Exception failureException) {
                log.error(
                        "분석 실패 상태를 저장하지 못했습니다. runId={}",
                        runId,
                        failureException
                );
            }
        }
    }
}