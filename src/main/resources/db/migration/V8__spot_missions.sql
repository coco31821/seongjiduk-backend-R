-- 스팟 미션/퀘스트 (SpotMission) — 시네마틱 여정(시사회)의 게임 루프 원천 데이터
-- 관련 문서: 기획/13_시네마틱_여정_퀘스트_설계.md
-- 생성: AI describe 파이프라인이 초안 저장(origin=AI), 관리자 검수(origin=ADMIN)
-- 주의: spot_id 는 pilgrimage_spots 참조지만 그 테이블이 아직 Flyway 관리 밖(ddl-auto 생성)이라
--       FK 를 걸지 않는다(V6/V7 컨벤션 동일 — 상대 테이블이 Flyway 확정 후 별도 마이그레이션에서 FK 추가).
CREATE TABLE spot_missions (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    spot_id      BIGINT       NOT NULL,
    title        VARCHAR(100) NOT NULL,
    description  VARCHAR(500) NOT NULL,
    mission_type VARCHAR(20)  NOT NULL,         -- FIND | PHOTO | TASTE | EXPERIENCE
    origin       VARCHAR(20)  NOT NULL,         -- AI | ADMIN
    active       BOOLEAN      NOT NULL          -- 관리자 검수로 숨김 처리(비활성) 가능
);

-- 스팟별 조회(existsBySpotId·findBySpotIdOrderByIdAsc) + 작품 단위 일괄 조회(spot join)용
CREATE INDEX idx_spot_missions_spot_id ON spot_missions (spot_id);
