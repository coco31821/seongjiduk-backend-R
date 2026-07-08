-- 방문 인증/메모 (VisitRecord) — S-3 시나리오, 성지 여권의 원천 데이터
-- 관련 문서: 기획/04_도메인_모델_ERD.md
-- 주의: user_id / spot_id / trip_plan_id 는 다른 도메인(A/B파트) 테이블 참조지만
--       users·pilgrimage_spots 가 아직 Flyway 관리 밖(ddl-auto 생성)이라 FK를 걸지 않는다.
--       (V2/V3 컨벤션 동일 — 상대 테이블이 Flyway로 확정된 뒤 별도 마이그레이션에서 FK 추가.)
CREATE TABLE visit_record (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id      BIGINT        NOT NULL,
    spot_id      BIGINT        NOT NULL,
    trip_plan_id BIGINT,                       -- 일정 없이 인증만 남길 수 있어 nullable
    note         VARCHAR(1000),
    image_url    VARCHAR(500),
    visited_at   TIMESTAMP     NOT NULL
);

-- 내 방문 기록 조회(findByUserIdOrderByVisitedAtDesc) + 성지 여권 작품별 집계용
CREATE INDEX idx_visit_record_user_id ON visit_record (user_id);
CREATE INDEX idx_visit_record_spot_id ON visit_record (spot_id);
