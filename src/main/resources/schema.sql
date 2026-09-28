CREATE TABLE IF NOT EXISTS `search_session` (
    `id` BIGINT NOT NULL AUTO_INCREMENT
        COMMENT '탐색 세션 ID',

    `recovery_token_hash` CHAR(64)
        CHARACTER SET ascii COLLATE ascii_bin
        NOT NULL
        COMMENT '복구 토큰 해시',

    `last_lat` DECIMAL(10, 7) NOT NULL
        COMMENT '마지막 확인 위치 위도',

    `last_lng` DECIMAL(10, 7) NOT NULL
        COMMENT '마지막 확인 위치 경도',

    `last_address` VARCHAR(255) NULL DEFAULT NULL
        COMMENT '마지막 확인 위치 주소',

    `last_seen_at` DATETIME NOT NULL
        COMMENT '마지막 확인 시각',

    `person_attributes` JSON NOT NULL
        COMMENT '대상자 특성',

    `created_at` DATETIME NOT NULL
        COMMENT '탐색 세션 생성 시각',

    `expires_at` DATETIME NOT NULL
        COMMENT '탐색 세션 만료 시각',

    CONSTRAINT `pk_search_session`
        PRIMARY KEY (`id`),

    CONSTRAINT `uq_search_session_recovery_token`
        UNIQUE (`recovery_token_hash`),

    CONSTRAINT `chk_search_session_lat`
        CHECK (`last_lat` BETWEEN -90 AND 90),

    CONSTRAINT `chk_search_session_lng`
        CHECK (`last_lng` BETWEEN -180 AND 180),

    CONSTRAINT `chk_search_session_expiry`
        CHECK (`expires_at` > `created_at`),

    INDEX `idx_search_session_expires` (`expires_at`)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci
COMMENT = '기한부 탐색 세션';


CREATE TABLE IF NOT EXISTS `analysis_run` (
    `id` BIGINT NOT NULL AUTO_INCREMENT
        COMMENT '분석 실행 ID',

    `session_id` BIGINT NOT NULL
        COMMENT '탐색 세션 ID',

    `requested_at` DATETIME NOT NULL
        COMMENT '분석 요청 시각',

    `status` ENUM('PROCESSING', 'COMPLETED', 'FAILED')
        NOT NULL DEFAULT 'PROCESSING'
        COMMENT '분석 실행 상태',

    `completed_at` DATETIME NULL DEFAULT NULL
        COMMENT '분석 종료 시각',

    CONSTRAINT `pk_analysis_run`
        PRIMARY KEY (`id`),

    CONSTRAINT `fk_analysis_run_session`
        FOREIGN KEY (`session_id`)
        REFERENCES `search_session` (`id`)
        ON DELETE CASCADE
        ON UPDATE RESTRICT,

    INDEX `idx_analysis_run_session_status_requested`
        (`session_id`, `status`, `requested_at` DESC, `id` DESC)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci
COMMENT = '탐색 세션별 분석 실행';


CREATE TABLE IF NOT EXISTS `time_result` (
    `id` BIGINT NOT NULL AUTO_INCREMENT
        COMMENT '시간점 결과 ID',

    `run_id` BIGINT NOT NULL
        COMMENT '분석 실행 ID',

    `time_point` ENUM(
        'NOW',
        'AFTER_30M',
        'AFTER_1H',
        'AFTER_3H',
        'AFTER_6H'
    ) NOT NULL
        COMMENT '시간점 구분',

    `target_at` DATETIME NOT NULL
        COMMENT '예측 대상 시각',

    `boundary_zone` JSON NULL DEFAULT NULL
        COMMENT 'AI 예측 영역',

    `reliability_status` VARCHAR(30) NULL DEFAULT NULL
        COMMENT 'AI 신뢰도 상태',

    CONSTRAINT `pk_time_result`
        PRIMARY KEY (`id`),

    CONSTRAINT `uq_time_result_run_point`
        UNIQUE (`run_id`, `time_point`),

    CONSTRAINT `fk_time_result_run`
        FOREIGN KEY (`run_id`)
        REFERENCES `analysis_run` (`id`)
        ON DELETE CASCADE
        ON UPDATE RESTRICT
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci
COMMENT = '분석 실행의 시간점별 결과';


CREATE TABLE IF NOT EXISTS `priority_place` (
    `id` BIGINT NOT NULL AUTO_INCREMENT
        COMMENT '추천 장소 ID',

    `result_id` BIGINT NOT NULL
        COMMENT '시간점 결과 ID',

    `poi_id` VARCHAR(100) NOT NULL
        COMMENT '장소 식별자',

    `priority_rank` TINYINT NOT NULL
        COMMENT '추천 순위',

    `name` VARCHAR(150) NOT NULL
        COMMENT '장소명',

    `address` VARCHAR(255) NULL DEFAULT NULL
        COMMENT '장소 주소',

    `lat` DECIMAL(10, 7) NOT NULL
        COMMENT '장소 위도',

    `lng` DECIMAL(10, 7) NOT NULL
        COMMENT '장소 경도',

    `score` DECIMAL(10, 7) NULL DEFAULT NULL
        COMMENT 'AI 추천 점수',

    CONSTRAINT `pk_priority_place`
        PRIMARY KEY (`id`),

    CONSTRAINT `uq_priority_place_result_rank`
        UNIQUE (`result_id`, `priority_rank`),

    CONSTRAINT `uq_priority_place_result_poi`
        UNIQUE (`result_id`, `poi_id`),

    CONSTRAINT `fk_priority_place_result`
        FOREIGN KEY (`result_id`)
        REFERENCES `time_result` (`id`)
        ON DELETE CASCADE
        ON UPDATE RESTRICT,

    CONSTRAINT `chk_priority_place_rank`
        CHECK (`priority_rank` BETWEEN 1 AND 3),

    CONSTRAINT `chk_priority_place_lat`
        CHECK (`lat` BETWEEN -90 AND 90),

    CONSTRAINT `chk_priority_place_lng`
        CHECK (`lng` BETWEEN -180 AND 180)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci
COMMENT = '시간점별 TOP 3 추천 장소';


CREATE TABLE IF NOT EXISTS `place_check` (
    `id` BIGINT NOT NULL AUTO_INCREMENT
        COMMENT '확인 기록 ID',

    `place_id` BIGINT NOT NULL
        COMMENT '확인한 추천 장소 ID',

    `checked_at` DATETIME NOT NULL
        COMMENT '확인 완료 시각',

    CONSTRAINT `pk_place_check`
        PRIMARY KEY (`id`),

    CONSTRAINT `uq_place_check_place`
        UNIQUE (`place_id`),

    CONSTRAINT `fk_place_check_place`
        FOREIGN KEY (`place_id`)
        REFERENCES `priority_place` (`id`)
        ON DELETE CASCADE
        ON UPDATE RESTRICT
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci
COMMENT = '추천 장소 확인 완료 기록';


CREATE TABLE IF NOT EXISTS `snapshot` (
    `id` BIGINT NOT NULL AUTO_INCREMENT
        COMMENT '공유 스냅샷 ID',

    `session_id` BIGINT NULL DEFAULT NULL
        COMMENT '발급 출처 탐색 세션 ID',

    `token_hash` CHAR(64)
        CHARACTER SET ascii COLLATE ascii_bin
        NOT NULL
        COMMENT '공유 토큰 해시',

    `last_lat` DECIMAL(10, 7) NOT NULL
        COMMENT '공유 당시 마지막 확인 위치 위도',

    `last_lng` DECIMAL(10, 7) NOT NULL
        COMMENT '공유 당시 마지막 확인 위치 경도',

    `last_address` VARCHAR(255) NULL DEFAULT NULL
        COMMENT '공유 화면의 마지막 확인 위치 주소',

    `created_at` DATETIME NOT NULL
        COMMENT '공유 링크 발급 시각',

    `expires_at` DATETIME NOT NULL
        COMMENT '공유 링크 만료 시각',

    `revoked_at` DATETIME NULL DEFAULT NULL
        COMMENT '공유 링크 폐기 시각',

    CONSTRAINT `pk_snapshot`
        PRIMARY KEY (`id`),

    CONSTRAINT `uq_snapshot_token`
        UNIQUE (`token_hash`),

    CONSTRAINT `fk_snapshot_session`
        FOREIGN KEY (`session_id`)
        REFERENCES `search_session` (`id`)
        ON DELETE SET NULL
        ON UPDATE RESTRICT,

    CONSTRAINT `chk_snapshot_lat`
        CHECK (`last_lat` BETWEEN -90 AND 90),

    CONSTRAINT `chk_snapshot_lng`
        CHECK (`last_lng` BETWEEN -180 AND 180),

    CONSTRAINT `chk_snapshot_expiry`
        CHECK (
            `expires_at` > `created_at`
            AND `expires_at` <= DATE_ADD(`created_at`, INTERVAL 12 HOUR)
        ),

    INDEX `idx_snapshot_session` (`session_id`),
    INDEX `idx_snapshot_expires` (`expires_at`)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci
COMMENT = '12시간 이내 만료되는 공유 스냅샷';

CREATE TABLE IF NOT EXISTS `snapshot_place` (
    `id` BIGINT NOT NULL AUTO_INCREMENT
        COMMENT '공유 장소 ID',

    `snapshot_id` BIGINT NOT NULL
        COMMENT '공유 스냅샷 ID',

    `priority_rank` TINYINT NOT NULL
        COMMENT '공유 당시 추천 순위',

    `name` VARCHAR(150) NOT NULL
        COMMENT '공유 장소명',

    `address` VARCHAR(255) NULL DEFAULT NULL
        COMMENT '공유 장소 주소',

    `lat` DECIMAL(10, 7) NOT NULL
        COMMENT '공유 장소 위도',

    `lng` DECIMAL(10, 7) NOT NULL
        COMMENT '공유 장소 경도',

    CONSTRAINT `pk_snapshot_place`
        PRIMARY KEY (`id`),

    CONSTRAINT `uq_snapshot_place_snapshot_rank`
        UNIQUE (`snapshot_id`, `priority_rank`),

    CONSTRAINT `fk_snapshot_place_snapshot`
        FOREIGN KEY (`snapshot_id`)
        REFERENCES `snapshot` (`id`)
        ON DELETE CASCADE
        ON UPDATE RESTRICT,

    CONSTRAINT `chk_snapshot_place_rank`
        CHECK (`priority_rank` BETWEEN 1 AND 3),

    CONSTRAINT `chk_snapshot_place_lat`
        CHECK (`lat` BETWEEN -90 AND 90),

    CONSTRAINT `chk_snapshot_place_lng`
        CHECK (`lng` BETWEEN -180 AND 180)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci
COMMENT = '공유 당시 TOP 3 장소';