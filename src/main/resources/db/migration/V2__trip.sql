-- Trip 도메인 (B 파트) 스키마
-- 관련 문서: 기획/04_도메인_모델_ERD.md, 기획/05_API_명세.md
-- 주의: user_id / content_id / pilgrimage_spot_id / nearby_attraction_id 는
--       다른 도메인(A파트) 테이블이 아직 없어 지금은 FK 제약 없이 값만 보관한다.
--       상대 테이블 확정 후 별도 마이그레이션에서 FK를 추가한다.

CREATE TABLE trip_plan (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id        BIGINT,
    content_id     BIGINT       NOT NULL,
    title          VARCHAR(200) NOT NULL,
    start_location VARCHAR(200),
    duration_days  INT          NOT NULL,
    budget_level   VARCHAR(20),
    travel_style   VARCHAR(30),
    status         VARCHAR(20)  NOT NULL,
    share_token    VARCHAR(64),
    created_at     TIMESTAMP    NOT NULL,
    CONSTRAINT uk_trip_plan_share_token UNIQUE (share_token)
);
CREATE INDEX idx_trip_plan_user_id ON trip_plan (user_id);
CREATE INDEX idx_trip_plan_content_id ON trip_plan (content_id);

CREATE TABLE trip_day (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    trip_plan_id BIGINT       NOT NULL,
    day_no       INT          NOT NULL,
    summary      VARCHAR(500),
    CONSTRAINT fk_trip_day_plan FOREIGN KEY (trip_plan_id) REFERENCES trip_plan (id) ON DELETE CASCADE
);
CREATE INDEX idx_trip_day_plan_id ON trip_day (trip_plan_id);

CREATE TABLE trip_stop (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    trip_day_id          BIGINT      NOT NULL,
    spot_type            VARCHAR(20) NOT NULL,
    pilgrimage_spot_id   BIGINT,
    nearby_attraction_id BIGINT,
    seq_no               INT         NOT NULL,
    arrival_time         VARCHAR(10),
    stay_minutes         INT,
    CONSTRAINT fk_trip_stop_day FOREIGN KEY (trip_day_id) REFERENCES trip_day (id) ON DELETE CASCADE,
    -- spot_type 에 맞는 스팟 id 하나만 채워지도록 보장 (ERD 설계메모)
    CONSTRAINT ck_trip_stop_spot_ref CHECK (
        (spot_type = 'PILGRIMAGE' AND pilgrimage_spot_id IS NOT NULL AND nearby_attraction_id IS NULL)
        OR
        (spot_type = 'ATTRACTION' AND nearby_attraction_id IS NOT NULL AND pilgrimage_spot_id IS NULL)
    )
);
CREATE INDEX idx_trip_stop_day_id ON trip_stop (trip_day_id);
