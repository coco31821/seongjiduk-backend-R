# Anitabi 성지 데이터 임포트 — 설계 스펙

- 날짜: 2026-07-06
- 상태: 설계 확정(리뷰 대기)
- 관련: `기획/05_API_명세.md`, `기획/04_도메인_모델_ERD.md`, `기획/06_AI_에이전트_설계.md`

## 1. 목적 / 배경
관리자가 작품(Content)에 **Anitabi API**의 성지순례 포인트를 당겨와 `PilgrimageSpot`으로 채우는 임포트 기능.
성지 데이터(좌표·장면)는 지도 표시와 AI 일정 생성·설명의 토대다. 블로그 크롤링(저작권·품질·시간) 대신 구조화 API를 쓴다.

## 2. 범위
**In**
- 관리자 엔드포인트로 `bangumiId` 기반 Anitabi 포인트 → `PilgrimageSpot`(+출처 `SpotReference`) 저장.
- 좌표→주소 역지오코딩(스위처블: Google 기본 + fallback).
- `external_id` 기반 멱등 upsert(재import 안전 + provenance).

**Out (다음 트랙)**
- 주변 관광지(`NearbyAttraction`) 수집 — Places API/큐레이션.
- AI "장면 설명" 생성 — AI 레이어.
- 실제 이동시간·시간창·솔버 — AI 레이어.
- 체류시간/인기 신호 enrichment(`VisitRecord`) — 나중.

## 3. 선행조건 / 의존
- **#25 머지 완료** → `content`, `spot`(PilgrimageSpot·SpotReference) 도메인 엔티티/리포지토리 사용 가능.
- ADMIN 인증(#25 JWT) 동작 → `/api/admin/**` = `hasRole("ADMIN")`.
- ⚠️ 본 스펙은 A파트 `PilgrimageSpot`에 컬럼 2개 추가를 포함(§7) — **A파트 조율 필요.**

## 4. API 계약
```
POST /api/admin/contents/{contentId}/spots/import      (ADMIN)
Request  : { "bangumiId": 49294 }
Response : 200
{
  "contentId": 1,
  "bangumiId": 49294,
  "created": 30, "updated": 6, "geocodeFallback": 4, "failed": 0,
  "total": 36
}
```
- 성공 시 요약 반환. 부분 실패는 카운트로 표시(전체 실패 아님).

## 5. 아키텍처 / 구성요소 (각자 한 역할)
- **`AnitabiClient`** (spot/infra) — RestClient로 Anitabi 호출.
  - `fetchWork(bangumiId)` → `/bangumi/{id}/lite` (제목·city·중심좌표)
  - `fetchPoints(bangumiId)` → `/bangumi/{id}/points/detail?haveImage=true` → `List<AnitabiPoint>`
  - 내부 DTO만 노출(Anitabi 응답 형식 은닉).
- **`ReverseGeocoder`** (인터페이스) — `reverse(lat, lng) -> Optional<GeoResult{address, city}>`
  - `GoogleReverseGeocoder` — env `GOOGLE_MAPS_API_KEY` 있으면 호출, 없거나 실패면 `Optional.empty()`.
  - (키 없거나 실패 시 서비스가 fallback 처리 → import는 항상 동작)
- **`SpotImportService`** — 오케스트레이션(§6 흐름). `@Transactional`.
- **`AdminSpotImportController`** — 위 엔드포인트, ADMIN.

## 6. 데이터 흐름 & 매핑
```
Content(contentId) 조회 (없으면 404)
AnitabiClient.fetchWork(bangumiId)   → 작품 city(폴백용)
AnitabiClient.fetchPoints(bangumiId) → 포인트들
각 포인트:
  geo[lat,lng] → ReverseGeocoder → (address, city)   실패/키없음 → fallback(city=작품city, address=name)
  upsert PilgrimageSpot  key=(content, external_source="anitabi", external_id=point.id)
  SpotReference 갱신(sourceName="Anitabi")
요약 반환
```
| Anitabi 포인트 | PilgrimageSpot |
|---|---|
| name | name |
| geo[0], geo[1] | lat, lng (BigDecimal) |
| (역지오코딩) | address, city |
| — | recommendedDurationMin = **30**(기본) |
| originURL | referenceUrl |
| id | **external_id** (external_source="anitabi") |
| origin/originURL/image/ep | → `SpotReference`(sourceName="Anitabi", url=originURL, title="{name} EP{ep}") |

## 7. 스키마 변경 (⚠️ A파트 조율)
`pilgrimage_spot`에 provenance/멱등용 컬럼 추가 (마이그레이션 Vn):
```sql
ALTER TABLE pilgrimage_spot
  ADD COLUMN external_source VARCHAR(30),
  ADD COLUMN external_id     VARCHAR(64);
CREATE UNIQUE INDEX uk_spot_external
  ON pilgrimage_spot (content_id, external_source, external_id);
```
- `PilgrimageSpot` 엔티티에 `externalSource`, `externalId` 필드 + 세터/빌더 반영.
- **A파트 소유 테이블/엔티티** → PR 전 협의. (원치 않으면 (content,name) upsert로 임시 시작 가능하나 provenance 약화.)

## 8. 역지오코딩 (스위처블)
- **기본 목표 = Google Geocoding** (`https://maps.googleapis.com/maps/api/geocode/json?latlng=..&key=..&language=ja`). 서버 키(IP 제한), env `GOOGLE_MAPS_API_KEY`(커밋 금지).
- **키 없거나 실패 → fallback**: city=Anitabi 작품 city, address=포인트 name. (import는 항상 성공)
- 근거: 주소는 루트 계산엔 불필요(좌표면 됨), **AI 설명·Day 라벨 품질**엔 유용 → 옵션.
- Nominatim 공개서버는 정책상 403 → 사용 안 함.

## 9. 멱등 / 중복
- upsert 키 = `(content_id, external_source, external_id)`. 재import 시 기존 갱신, 신규만 추가.

## 10. 출처 / 라이선스
- Anitabi = **CC BY-NC-SA 4.0(비상업)**. `SpotReference`에 origin/originURL 저장, **이미지 재호스팅 금지(링크만)**.
- UI에 "Anitabi (CC BY-NC-SA 4.0)" 크레딧. **상용 전환 시 자체 데이터로 교체.**

## 11. 에러 처리
- Content 없음 → **`BusinessException`/`ErrorCode`(404)** (#25 `GlobalExceptionHandler` 모델에 맞춤).
- Anitabi 호출 실패 → 에러 응답(부분 오염 방지).
- 포인트별 지오코딩 실패 → fallback으로 계속, `geocodeFallback` 카운트.

## 12. 설정 (application.yml)
```yaml
seongjiduk:
  anitabi:
    base-url: ${ANITABI_BASE_URL:https://api.anitabi.cn}
  geocoding:
    google:
      api-key: ${GOOGLE_MAPS_API_KEY:}   # 비면 fallback
```

## 13. 테스트 (BDD)
- `SpotImportService` 테스트: `AnitabiClient`·`ReverseGeocoder`를 **BDDMockito**로 mock.
  - given/when/then: 저장(created), 재import upsert(updated), 지오코딩 실패→fallback, Content 없음→예외.
- **실제 외부 호출 없음.** 컨트롤러 web-slice 테스트는 classpath 미지원으로 보류.

## 14. 커밋 계획 (레이어/관심사별)
1. `[FEAT] PilgrimageSpot external_source/external_id 추가(+마이그레이션)` — A파트 조율
2. `[FEAT] AnitabiPoint DTO`
3. `[FEAT] AnitabiClient(RestClient)`
4. `[FEAT] ReverseGeocoder 인터페이스 + GoogleReverseGeocoder`
5. `[TEST] SpotImportService (실패)`
6. `[FEAT] SpotImportService 구현`
7. `[FEAT] AdminSpotImportController`
8. `[INFRA] anitabi/geocoding 설정`

## 15. 향후 (seam)
- 주변 관광지(NearbyAttraction) 트랙, AI 장면 설명(AI 레이어), 체류시간·인기 신호 enrichment(`VisitRecord` 플라이휠).
