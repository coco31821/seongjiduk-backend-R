-- 이벤트 수집 (접속/이용 통계 원천)
-- 관련 문서: 기획/05_API_명세.md(POST /api/events, EVENT-001), 기획/07_관리자_통계_설계.md
-- user_id는 다른 도메인(User) 테이블 확정 후 별도 마이그레이션에서 FK 추가한다.

CREATE TABLE usage_event (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT,
    event_type  VARCHAR(30)  NOT NULL,
    path        VARCHAR(255),
    target_id   BIGINT,
    session_id  VARCHAR(64),
    occurred_at TIMESTAMP    NOT NULL
);

-- 통계 집계(타입별 일자 카운트, 기간 조회)를 위한 인덱스
CREATE INDEX idx_usage_event_type_time ON usage_event (event_type, occurred_at);
CREATE INDEX idx_usage_event_occurred_at ON usage_event (occurred_at);
