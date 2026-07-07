-- 해리포터 (영국, MOVIE) — Wikidata P915 촬영지 + 유명 누락지 수동 보강. 멱등.
INSERT INTO contents (title, category, country, description)
SELECT '해리포터','MOVIE','GB','런던에서 스코틀랜드까지, 8편의 영화 촬영지를 따라가는 마법 성지순례'
WHERE NOT EXISTS (SELECT 1 FROM contents WHERE title='해리포터');

SET @hp := (SELECT id FROM contents WHERE title='해리포터' LIMIT 1);

INSERT INTO pilgrimage_spots (content_id, name, city, address, lat, lng, recommended_duration_min)
SELECT @hp, t.name, t.city, t.address, t.lat, t.lng, t.dur FROM (
  SELECT '킹스크로스 역 9¾ 승강장' name,'London' city,'King''s Cross Station, Euston Rd, London' address,51.5322000 lat,-0.1236000 lng,30 dur UNION ALL
  SELECT '레든홀 마켓','London','Leadenhall Market, Gracechurch St, London',51.5127000,-0.0835000,30 UNION ALL
  SELECT '밀레니엄 브리지','London','Millennium Bridge, London',51.5103000,-0.0784000,15 UNION ALL
  SELECT '런던 동물원 파충류관','London','ZSL London Zoo, Outer Cir, London',51.5356000,-0.1558000,60 UNION ALL
  SELECT '워너브라더스 스튜디오 투어 (리브즈든)','Watford','Warner Bros. Studio Tour London, Leavesden',51.6933000,-0.4197000,240 UNION ALL
  SELECT '크라이스트 처치','Oxford','Christ Church, St Aldate''s, Oxford',51.7502000,-1.2559000,60 UNION ALL
  SELECT '옥스퍼드 뉴 칼리지','Oxford','New College, Holywell St, Oxford',51.7542000,-1.2511000,45 UNION ALL
  SELECT '글로스터 대성당','Gloucester','Gloucester Cathedral, College Green',51.8673000,-2.2465000,45 UNION ALL
  SELECT '더럼 대성당','Durham','Durham Cathedral, The College, Durham',54.7735000,-1.5762000,45 UNION ALL
  SELECT '안윅 성','Alnwick','Alnwick Castle, Alnwick, Northumberland',55.4158000,-1.7061000,90 UNION ALL
  SELECT '고스랜드 역','Goathland','Goathland Station, North Yorkshire',54.4007000,-0.7121000,30 UNION ALL
  SELECT '글렌피넌 육교','Glenfinnan','Glenfinnan Viaduct, Inverness-shire',56.8763000,-5.4312000,60 UNION ALL
  SELECT '글렌 네비스','Fort William','Glen Nevis, Fort William, Highland',56.7706000,-5.0356000,60
) t
WHERE NOT EXISTS (
  SELECT 1 FROM pilgrimage_spots s WHERE s.content_id=@hp AND s.name=t.name
);
