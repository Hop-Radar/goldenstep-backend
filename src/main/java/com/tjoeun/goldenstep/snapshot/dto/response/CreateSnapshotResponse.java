package com.tjoeun.goldenstep.snapshot.dto.response;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Getter;

@Getter
@Schema(description = "공유 스냅샷 발급 결과")
public class CreateSnapshotResponse {

    @Schema(description = "공유 스냅샷 ID", example = "1")
    private final Long snapshotId;

    @Schema(description = "공유 화면 URL", example = "https://example.com/share-map.html#token=SHARE_TOKEN")
    private final String shareUrl;

    @Schema(description = "공유 링크 발급 시각 (한국 시각)", example = "2026-10-04T12:00:00")
    private final LocalDateTime createdAt;

    @Schema(description = "공유 링크 만료 시각 (발급 후 12시간)", example = "2026-10-05T00:00:00")
    private final LocalDateTime expiresAt;

    public CreateSnapshotResponse(
            Long snapshotId,
            String shareUrl,
            LocalDateTime createdAt,
            LocalDateTime expiresAt
    ) {
        this.snapshotId = snapshotId;
        this.shareUrl = shareUrl;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }
}
