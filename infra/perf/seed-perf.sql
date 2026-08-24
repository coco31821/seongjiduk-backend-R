-- Deterministic, external-API-free dataset for k6 CONTENT_ID=1.
INSERT INTO contents (id, title, category, country, description)
VALUES (1, 'Redis Performance Fixture', 'ANIME', 'JP', 'Fixed content for reproducible Redis experiments')
ON DUPLICATE KEY UPDATE title=VALUES(title);

INSERT INTO pilgrimage_spots (id, content_id, name, address, lat, lng, city, recommended_duration_min)
VALUES
  (1, 1, 'Mock Shrine A', 'Mock Address A', 35.6810000, 139.7670000, 'Tokyo', 45),
  (2, 1, 'Mock Shrine B', 'Mock Address B', 35.6820000, 139.7680000, 'Tokyo', 45)
ON DUPLICATE KEY UPDATE name=VALUES(name);
