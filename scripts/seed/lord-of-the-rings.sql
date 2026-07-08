-- 반지의 제왕 (뉴질랜드, MOVIE) — Wikidata P915 촬영지 실측 + 유명 누락지 수동 보강. 멱등.
INSERT INTO contents (title, category, country, description)
SELECT '반지의 제왕','MOVIE','NZ','호비턴에서 모르도르까지, 뉴질랜드 전역의 중간계 촬영지 순례'
WHERE NOT EXISTS (SELECT 1 FROM contents WHERE title='반지의 제왕');

SET @lotr := (SELECT id FROM contents WHERE title='반지의 제왕' LIMIT 1);

INSERT INTO pilgrimage_spots (content_id, name, city, address, lat, lng, recommended_duration_min)
SELECT @lotr, t.name, t.city, t.address, t.lat, t.lng, t.dur FROM (
  SELECT '호비턴 무비 세트' name,'Matamata' city,'501 Buckland Rd, Hinuera, Matamata' address,-37.8575000 lat,175.6797200 lng,180 dur UNION ALL
  SELECT '마운트 선데이 (에도라스)','Canterbury','Mount Sunday, Ashburton Lakes, Canterbury',-43.5480000,170.8930000,90 UNION ALL
  SELECT '스키퍼스 캐니언','Queenstown','Skippers Canyon, Queenstown',-44.8789000,168.6260000,90 UNION ALL
  SELECT '파라다이스 (로스로리엔)','Glenorchy','Paradise, Glenorchy, Otago',-44.7235100,168.3641600,60 UNION ALL
  SELECT '글레노키 (아이센가드)','Glenorchy','Glenorchy, Otago',-44.8500000,168.3833300,45 UNION ALL
  SELECT '피오르드랜드 국립공원','Te Anau','Fiordland National Park, Southland',-45.3860000,167.3470000,120 UNION ALL
  SELECT '마보라 호수','Southland','Mavora Lakes, Southland',-45.2555600,168.1680600,60 UNION ALL
  SELECT '마운트 오웬 (모리아)','Tasman','Mount Owen, Kahurangi National Park',-41.5520000,172.5410000,90 UNION ALL
  SELECT '마운트 올림푸스','Canterbury','Mount Olympus, Canterbury',-43.1924500,171.6063400,90 UNION ALL
  SELECT '통가리로 국립공원 (모르도르)','Tongariro','Tongariro National Park, Manawatu-Whanganui',-39.2000000,175.5800000,120 UNION ALL
  SELECT '웨타 워크숍','Wellington','1 Weka St, Miramar, Wellington',-41.3049000,174.8236000,90
) t
WHERE NOT EXISTS (
  SELECT 1 FROM pilgrimage_spots s WHERE s.content_id=@lotr AND s.name=t.name
);
