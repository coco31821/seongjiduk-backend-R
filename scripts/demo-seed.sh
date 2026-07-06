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
BANGUMI_ID="${BANGUMI_ID:-49294}"   # 러브라이브!

echo "[1/4] 관리자 가입 (이미 있으면 무시)"
curl -sf -X POST "$BASE_URL/api/auth/signup" -H "Content-Type: application/json" \
  -d "{\"email\":\"$ADMIN_EMAIL\",\"password\":\"$ADMIN_PASSWORD\",\"nickname\":\"데모관리자\"}" > /dev/null || true

echo "[2/4] ADMIN 승격 + 작품 등록"
docker exec "$MYSQL_CONTAINER" mysql --default-character-set=utf8mb4 \
  -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DATABASE" -e \
  "UPDATE users SET role='ADMIN' WHERE email='$ADMIN_EMAIL';
   INSERT INTO contents (title, category, country, description)
   SELECT '러브라이브!','ANIME','JP','도쿄 아키하바라 성지순례'
   WHERE NOT EXISTS (SELECT 1 FROM contents WHERE title='러브라이브!');"

echo "[3/4] 로그인 → Anitabi 임포트"
TOKEN=$(curl -sf -X POST "$BASE_URL/api/auth/login" -H "Content-Type: application/json" \
  -d "{\"email\":\"$ADMIN_EMAIL\",\"password\":\"$ADMIN_PASSWORD\"}" \
  | python3 -c "import json,sys; print(json.load(sys.stdin)['data']['accessToken'])")
CONTENT_ID=$(docker exec "$MYSQL_CONTAINER" mysql -N -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DATABASE" \
  -e "SELECT id FROM contents WHERE title='러브라이브!' LIMIT 1;")
curl -sf -X POST "$BASE_URL/api/admin/contents/$CONTENT_ID/spots/import" \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d "{\"bangumiId\":$BANGUMI_ID}" | python3 -m json.tool

echo "[4/4] 완료 — 임포트가 AI 설명 프리웜을 자동 트리거함(1~2분 뒤 첫 조회도 즉시)."
echo "확인: $BASE_URL/api/contents"
