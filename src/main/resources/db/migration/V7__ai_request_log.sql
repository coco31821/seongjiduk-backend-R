-- AI 호출 로그 (AiRequestLog) — B파트가 생성/재생성 시 기록, C파트(관리자 통계)가 읽기 전용 집계
-- 관련 문서: 기획/04_도메인_모델_ERD.md, 기획/07_관리자_통계_설계.md
-- 주의: trip_plan_id 는 '소프트 참조'다(엔티티도 @ManyToOne 아닌 Long) — 일정 삭제가
--       FK에 막혀 통계 로그가 소실되던 결함(#66) 때문에 의도적으로 FK를 걸지 않는다.
--       user_id 도 users 가 Flyway 관리 밖이라 FK 없이 값만 보관(V2/V3 컨벤션).
CREATE TABLE ai_request_log (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id      BIGINT,                        -- 비회원 생성 가능 → nullable
    trip_plan_id BIGINT,                        -- 소프트 참조 (FK 없음, 의도적)
    request_type VARCHAR(30)  NOT NULL,         -- TRIP_GENERATE | TRIP_REGENERATE
    status       VARCHAR(20)  NOT NULL,         -- SUCCESS | FALLBACK
    token_usage  INT,                           -- ai-service 미반환 → nullable(후속 확장)
    created_at   TIMESTAMP    NOT NULL
);

-- 관리자 통계 기간 집계(countByCreatedAtBetween)용
CREATE INDEX idx_ai_request_log_created_at ON ai_request_log (created_at);
