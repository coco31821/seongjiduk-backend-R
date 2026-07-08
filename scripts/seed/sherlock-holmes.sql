-- 셜록 홈즈 (영국, NOVEL) — 원작 소설 배경 + BBC 드라마 성지 큐레이션. 멱등.
INSERT INTO contents (title, category, country, description)
SELECT '셜록 홈즈','NOVEL','GB','베이커가 221B에서 시작하는 런던 원작 소설·드라마 성지순례'
WHERE NOT EXISTS (SELECT 1 FROM contents WHERE title='셜록 홈즈');

SET @sh := (SELECT id FROM contents WHERE title='셜록 홈즈' LIMIT 1);

INSERT INTO pilgrimage_spots (content_id, name, city, address, lat, lng, recommended_duration_min)
SELECT @sh, t.name, t.city, t.address, t.lat, t.lng, t.dur FROM (
  SELECT '셜록 홈즈 박물관 (221B 베이커가)' name,'London' city,'221B Baker St, London' address,51.5238000 lat,-0.1586000 lng,60 dur UNION ALL
  SELECT '세인트 바솔로뮤 병원','London','St Bartholomew''s Hospital, W Smithfield, London',51.5175000,-0.1000000,30 UNION ALL
  SELECT '크라이테리온 (피카딜리)','London','224 Piccadilly, London',51.5099000,-0.1343000,30 UNION ALL
  SELECT '심슨스 인 더 스트랜드','London','100 Strand, London',51.5107000,-0.1180000,60 UNION ALL
  SELECT '셜록 홈즈 펍','London','10-11 Northumberland St, London',51.5070000,-0.1250000,45 UNION ALL
  SELECT '스피디스 카페 (BBC 221B)','London','187 N Gower St, London',51.5258000,-0.1345000,30 UNION ALL
  SELECT '뉴 스코틀랜드 야드','London','Victoria Embankment, London',51.5023000,-0.1246000,15 UNION ALL
  SELECT '리젠트 파크','London','Regent''s Park, London',51.5313000,-0.1570000,45
) t
WHERE NOT EXISTS (
  SELECT 1 FROM pilgrimage_spots s WHERE s.content_id=@sh AND s.name=t.name
);
