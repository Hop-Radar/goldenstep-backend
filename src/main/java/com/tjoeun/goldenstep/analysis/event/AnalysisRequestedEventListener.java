package com.tjoeun.goldenstep.analysis.event;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.tjoeun.goldenstep.ai.dto.request.AnalysisPredictionRequest;
import com.tjoeun.goldenstep.analysis.service.AnalysisRunService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class AnalysisRequestedEventListener {

    private final AnalysisRunService analysisRunService;
    private final ThreadPoolTaskExecutor analysisExecutor;

    public AnalysisRequestedEventListener(
            AnalysisRunService analysisRunService,
            @Qualifier("analysisExecutor")
            ThreadPoolTaskExecutor analysisExecutor
    ) {
        this.analysisRunService = analysisRunService;
        this.analysisExecutor = analysisExecutor;
    }

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(AnalysisRequestedEvent event) {
        Long runId = event.getRunId();

        try {
			analysisExecutor.execute(() -> executeAnalysis(runId));

        } catch (TaskRejectedException exception) {
            log.error(
                    "분석 작업을 등록하지 못했습니다. "
                    + "실행기 포화 또는 종료 상태를 확인해주세요. runId={}",
                    runId,
                    exception
            );

            markFailed(runId);
        }
    }

    private void executeAnalysis(Long runId) {
        try {
			AnalysisPredictionRequest request = analysisRunService.createInitialPredictionRequest(runId);

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

            markFailed(runId);
        }
    }

    private void markFailed(Long runId) {
        try {
            analysisRunService.markInitialAnalysisFailed(runId);

        } catch (Exception exception) {
            log.error(
                    "분석 실패 상태를 저장하지 못했습니다. runId={}",
                    runId,
                    exception
            );
        }
    }
}