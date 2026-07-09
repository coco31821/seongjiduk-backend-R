#!/usr/bin/env bash
# 데모용 시드 — create-drop 환경에서 백엔드 (재)기동 후 한 번 실행하면 데모 데이터가 복구된다.
# 하는 일: 관리자 가입→승격 → 작품 등록 → Anitabi 임포트(러브라이브) → AI 설명 프리웜은 임포트가 자동 트리거.
# 사용: BASE_URL=https://api.holymoly.cloud MYSQL_CONTAINER=seongjiduk-mysql ./scripts/demo-seed.sh
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
MYSQL_CONTAINER="${MYSQL_CONTAINER:-seongjiduk-mysql}"
MYSQL_USER="${MYSQL_USER:-seongjiduk}"
MYSQL_PASSWORD="${MYSQL_PASSWORD:?MYSQL_PASSWORD 필요}"
MYSQL_DATABASE="${MYSQL_DATABASE:-seongjiduk}"
ADMIN_EMAIL="${ADMIN_EMAIL:-admin@seongjiduk.demo}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:?ADMIN_PASSWORD 필요}"
# 기수별 시드: "타이틀|설명|bangumiId" (Anitabi subject = bgm.tv 작품 ID, 기수별 분리)
CONTENTS=(
  "러브라이브!|도쿄 아키하바라 성지순례 (μ's)|49294"
  "러브라이브! 선샤인!!|누마즈를 무대로 한 Aqours의 성지순례|165553"
  "러브라이브! 니지가사키|도쿄 오다이바를 무대로 한 니지동 성지순례|296659"
  "데이트 어 라이브|마치다·타마 일대를 무대로 한 텐구시 성지순례|49131"
  "극장판 체인소맨: 레제편|간다 일대를 무대로 한 레제편 성지순례|470660"
  "스즈메의 문단속|규슈에서 도호쿠까지, 스즈메의 여정을 따라가는 로드무비 성지순례|362577"
  "너의 이름은.|도쿄와 히다 다카야마를 잇는 미츠하와 타키의 성지순례|160209"
  "슬램덩크|쇼난 해변과 가마쿠라 건널목의 그 장면|1608"
  "주술회전|도쿄 시부야·주술고전의 무대|294993"
  "하이큐!!|센다이 카라스노의 배구 성지|84171"
  "죠죠의 기묘한 모험: 다이아몬드는 부서지지 않는다|모리오초의 모델 센다이|150490"
)

echo "[1/4] 관리자 가입 (이미 있으면 무시)"
curl -sf -X POST "$BASE_URL/api/auth/signup" -H "Content-Type: application/json" \
  -d "{\"email\":\"$ADMIN_EMAIL\",\"password\":\"$ADMIN_PASSWORD\",\"nickname\":\"데모관리자\"}" > /dev/null || true

echo "[2/4] ADMIN 승격"
docker exec "$MYSQL_CONTAINER" mysql --default-character-set=utf8mb4 \
  -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DATABASE" -e \
  "UPDATE users SET role='ADMIN' WHERE email='$ADMIN_EMAIL';"

echo "[3/4] 로그인 → 기수별 작품 등록(멱등) + Anitabi 임포트"
TOKEN=$(curl -sf -X POST "$BASE_URL/api/auth/login" -H "Content-Type: application/json" \
  -d "{\"email\":\"$ADMIN_EMAIL\",\"password\":\"$ADMIN_PASSWORD\"}" \
  | python3 -c "import json,sys; print(json.load(sys.stdin)['data']['accessToken'])")

for entry in "${CONTENTS[@]}"; do
  IFS='|' read -r TITLE DESC BANGUMI <<< "$entry"
  # SQL 이스케이프 (μ's 아포스트로피 대응) — ESCAPED
  T_SQL=$(printf %s "$TITLE" | sed "s/'/''/g")
  D_SQL=$(printf %s "$DESC" | sed "s/'/''/g")
  docker exec "$MYSQL_CONTAINER" mysql --default-character-set=utf8mb4 \
    -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DATABASE" -e \
    "INSERT INTO contents (title, category, country, description)
     SELECT '$T_SQL','ANIME','JP','$D_SQL'
     WHERE NOT EXISTS (SELECT 1 FROM contents WHERE title='$T_SQL');"
  CONTENT_ID=$(docker exec "$MYSQL_CONTAINER" mysql -N --default-character-set=utf8mb4 \
    -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DATABASE" \
    -e "SELECT id FROM contents WHERE title='$T_SQL' LIMIT 1;")
  echo "  → $TITLE (content=$CONTENT_ID, bangumi=$BANGUMI)"
  curl -sf -X POST "$BASE_URL/api/admin/contents/$CONTENT_ID/spots/import" \
    -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
    -d "{\"bangumiId\":$BANGUMI}" | python3 -c "import json,sys; d=json.load(sys.stdin)['data']; print('    임포트:', d)"
done

# 알려진 원본 오태깅 제거: 무인편(49294)에 선샤인 소재 '海軍淡島桟橋'가 섞여 있음 (admin delete가 아직 mock이라 DB 직접)
# ⚠️ 자식 FK(spot_references·spot_missions)를 먼저 지워야 pilgrimage_spots 삭제 가능 (describe 프리웜이 미션 생성 → FK 막힘)
docker exec "$MYSQL_CONTAINER" mysql --default-character-set=utf8mb4 \
  -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DATABASE" -e \
  "DELETE sr FROM spot_references sr JOIN pilgrimage_spots ps ON sr.spot_id=ps.id
     WHERE ps.name='海軍淡島桟橋' AND ps.content_id=(SELECT id FROM contents WHERE title='러브라이브!' LIMIT 1);
   DELETE sm FROM spot_missions sm JOIN pilgrimage_spots ps ON sm.spot_id=ps.id
     WHERE ps.name='海軍淡島桟橋' AND ps.content_id=(SELECT id FROM contents WHERE title='러브라이브!' LIMIT 1);
   DELETE FROM pilgrimage_spots
     WHERE name='海軍淡島桟橋' AND content_id=(SELECT id FROM contents WHERE title='러브라이브!' LIMIT 1);"

echo "[3.5/4] 해리포터 (영국, MOVIE) — Wikidata 촬영지 큐레이션 시드 (멱등)"
docker exec -i "$MYSQL_CONTAINER" mysql --default-character-set=utf8mb4 \
  -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DATABASE" < "$(dirname "$0")/seed/harry-potter.sql"

echo "[3.6/4] 반지의 제왕 (뉴질랜드, MOVIE) + 셜록 홈즈 (영국, NOVEL) — 장르 확장 시드 (멱등)"
docker exec -i "$MYSQL_CONTAINER" mysql --default-character-set=utf8mb4 \
  -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DATABASE" < "$(dirname "$0")/seed/lord-of-the-rings.sql"
docker exec -i "$MYSQL_CONTAINER" mysql --default-character-set=utf8mb4 \
  -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DATABASE" < "$(dirname "$0")/seed/sherlock-holmes.sql"

echo "[3.7/4] BTS (한국, KPOP) — TourAPI 실좌표 + 수동 보강 시드 (멱등)"
docker exec -i "$MYSQL_CONTAINER" mysql --default-character-set=utf8mb4 \
  -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DATABASE" < "$(dirname "$0")/seed/bts.sql"

echo "[4/4] 완료 — 3기수 시드. 임포트가 AI 설명 프리웜을 자동 트리거함(수 분 뒤 첫 조회도 즉시)."
echo "확인: $BASE_URL/api/contents"
