-- Content / pilgrimage spot 도메인 스키마를 Flyway 관리 대상으로 편입한다.
-- 기존에는 ddl-auto가 만들던 테이블이라 validate 전환 시 누락 테이블로 잡힌다.

CREATE TABLE contents (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    title       VARCHAR(100) NOT NULL,
    category    VARCHAR(30)  NOT NULL,
    country     VARCHAR(50)  NOT NULL,
    description TEXT
);

CREATE TABLE pilgrimage_spots (
    id                       BIGINT AUTO_INCREMENT PRIMARY KEY,
    content_id               BIGINT        NOT NULL,
    name                     VARCHAR(100)  NOT NULL,
    address                  VARCHAR(255)  NOT NULL,
    lat                      DECIMAL(10,7) NOT NULL,
    lng                      DECIMAL(10,7) NOT NULL,
    city                     VARCHAR(50)   NOT NULL,
    recommended_duration_min INT           NOT NULL,
    reference_url            VARCHAR(500),
    CONSTRAINT fk_pilgrimage_spots_content FOREIGN KEY (content_id) REFERENCES contents (id)
);
CREATE INDEX idx_pilgrimage_spots_content_id ON pilgrimage_spots (content_id);
CREATE INDEX idx_pilgrimage_spots_city ON pilgrimage_spots (city);

CREATE TABLE spot_references (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    spot_id     BIGINT       NOT NULL,
    title       VARCHAR(100) NOT NULL,
    url         VARCHAR(500) NOT NULL,
    source_name VARCHAR(100),
    CONSTRAINT fk_spot_references_spot FOREIGN KEY (spot_id) REFERENCES pilgrimage_spots (id) ON DELETE CASCADE
);
CREATE INDEX idx_spot_references_spot_id ON spot_references (spot_id);
