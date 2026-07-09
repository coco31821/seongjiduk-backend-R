-- trip_stop.created_at 보정 — 엔티티 TripStop.createdAt은 있으나 V2에 컬럼 누락(create-drop이라 미검출).
-- validate 전환(EC2 배포)의 정합을 위해 추가. 다른 created_at(TIMESTAMP) 컨벤션 준수.
ALTER TABLE trip_stop ADD COLUMN created_at TIMESTAMP;
