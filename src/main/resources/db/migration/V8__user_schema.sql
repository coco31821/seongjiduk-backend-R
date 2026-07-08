-- User 도메인 스키마를 Flyway 관리 대상으로 편입한다.
-- 기존 trip_plan / usage_event / visit_record / ai_request_log 의 user_id 소프트 참조도 FK로 정리한다.

CREATE TABLE users (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    email         VARCHAR(50)  NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    nickname      VARCHAR(30)  NOT NULL,
    role          VARCHAR(20)  NOT NULL,
    created_at    TIMESTAMP    NOT NULL,
    updated_at    TIMESTAMP    NOT NULL,
    deleted_at    TIMESTAMP,
    CONSTRAINT uk_users_email UNIQUE (email)
);

CREATE TABLE user_preferences (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id      BIGINT NOT NULL,
    travel_style VARCHAR(50),
    budget_level VARCHAR(30),
    CONSTRAINT fk_user_preferences_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);
CREATE INDEX idx_user_preferences_user_id ON user_preferences (user_id);

ALTER TABLE trip_plan
    ADD CONSTRAINT fk_trip_plan_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE SET NULL;

ALTER TABLE usage_event
    ADD CONSTRAINT fk_usage_event_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE SET NULL;
CREATE INDEX idx_usage_event_user_id ON usage_event (user_id);

ALTER TABLE visit_record
    ADD CONSTRAINT fk_visit_record_user FOREIGN KEY (user_id) REFERENCES users (id);

ALTER TABLE ai_request_log
    ADD CONSTRAINT fk_ai_request_log_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE SET NULL;
CREATE INDEX idx_ai_request_log_user_id ON ai_request_log (user_id);
