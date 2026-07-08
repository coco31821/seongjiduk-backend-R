-- BTS (한국, KPOP) — 한국관광공사 TourAPI(searchKeyword) 실좌표 + 유명 성지 수동 보강. 멱등.
INSERT INTO contents (title, category, country, description)
SELECT 'BTS (방탄소년단)','KPOP','KR','주문진 버스정류장에서 하이브까지, 아미의 국내 성지순례'
WHERE NOT EXISTS (SELECT 1 FROM contents WHERE title='BTS (방탄소년단)');

SET @bts := (SELECT id FROM contents WHERE title='BTS (방탄소년단)' LIMIT 1);

INSERT INTO pilgrimage_spots (content_id, name, city, address, lat, lng, recommended_duration_min)
SELECT @bts, t.name, t.city, t.address, t.lat, t.lng, t.dur FROM (
  -- TourAPI 실데이터
  SELECT 'BTS 버스정류장 (주문진)' name,'강릉' city,'강원특별자치도 강릉시 주문진읍 주문북로 222-30' address,37.9113920 lat,128.8178558 lng,45 dur UNION ALL
  SELECT '케이팝 스퀘어 홍대','서울','서울특별시 마포구 양화로 141 (동교동)',37.5557535,126.9216695,60 UNION ALL
  SELECT '광주 충장로 케이팝 스타의 거리','광주','전남광주통합특별시 동구 충장로 94',35.1476177,126.9172387,45 UNION ALL
  -- 유명 성지 수동 보강
  SELECT '향호해변','강릉','강원특별자치도 강릉시 주문진읍 향호리',37.9169000,128.8155000,45 UNION ALL
  SELECT '하이브(HYBE) 사옥','서울','서울특별시 용산구 한강대로 42',37.5250000,126.9648000,30 UNION ALL
  SELECT '유정식당','서울','서울특별시 강남구 도산대로28길 8',37.5216000,127.0248000,60 UNION ALL
  SELECT '청구빌딩 (구 빅히트 사옥)','서울','서울특별시 강남구 도산대로16길 13-20',37.5187000,127.0207000,20 UNION ALL
  SELECT '일영역 (봄날 MV)','양주','경기도 양주시 장흥면 일영리',37.7249000,126.9427000,60
) t
WHERE NOT EXISTS (
  SELECT 1 FROM pilgrimage_spots s WHERE s.content_id=@bts AND s.name=t.name
);
