package com.tjoeun.goldenstep.search.handler;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.tjoeun.goldenstep.search.service.SearchSessionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "goldenstep.search.cleanup",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class SearchSessionCleanupHandler {
	
	private static final int MAX_BATCH_COUNT = 10;

    private final SearchSessionService searchSessionService;

    @Scheduled(
            initialDelayString =
                    "${goldenstep.search.cleanup.initial-delay-ms:60000}",
            fixedDelayString =
                    "${goldenstep.search.cleanup.fixed-delay-ms:60000}"
    )
    public void cleanupExpiredSessions() {
    	int totalDeletedCount = 0;
        int completedBatchCount = 0;
    	
        try {
            for (int batch = 0; batch < MAX_BATCH_COUNT; batch++) {
				int deletedCount = searchSessionService.deleteExpiredSessions();

                totalDeletedCount += deletedCount;
                completedBatchCount++;

                if (deletedCount < SearchSessionService.CLEANUP_BATCH_SIZE) {
                    break;
                }
            }

            if (totalDeletedCount > 0) {
                log.info(
                        "만료된 탐색 세션 {}개를 삭제했습니다. 실행한 배치 수: {}",
                        totalDeletedCount,
                        completedBatchCount
                );
            }
        } catch (Exception exception) {
            log.error(
                    "만료된 탐색 세션 자동 삭제에 실패했습니다. "
                            + "이번 실행에서 이미 삭제한 세션 수: {}. "
                            + "다음 실행에서 다시 시도합니다.",
                    totalDeletedCount,
                    exception
            );
        }
    }
}