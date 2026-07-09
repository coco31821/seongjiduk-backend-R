# 인수인계 — content·spot(A파트) 조회 계층

> 작성: teobasaki(B파트) · 대상: A파트(content·spot 도메인 담당)
> 목적: 프로젝트 초기 "기본 API 호출 골격"(커밋 `d039f4f`, 7/1)에서 **B파트인 내가 content·spot의 컨트롤러/서비스/DTO 스켈레톤까지 한꺼번에 깔아** A파트 조회 API를 선점한 상태를 바로잡기 위한 인수인계. 엔티티는 A파트가 만든 게 맞고, 그 위 조회 계층을 A파트가 자유롭게 재작성/소유하도록 넘긴다.

## 1. 현재 소유권 (blame 기준, 이메일 정규화)

| 구분 | A파트 소유(재작성 자유) | B파트(teobasaki) 기능 — **건드리면 깨짐** |
|---|---|---|
| **엔티티/모델** | `Content`·`PilgrimageSpot`·`SpotReference` (A파트 것) | — |
| **content** | (조회 API — 아래 §2 넘김) | `ContentService`의 describe 병합·prewarm(§3) |
| **spot** | `SpotDetailResponse`·성지제보 | 블로그 검증·Anitabi 임포트·주변장소·StreetView·describe(§4) |

> 원시 수치는 A파트 디렉터리에서 내 라인이 85%로 나오지만, 대부분은 **spot/ 패키지에 얹힌 내 고유 기능**(검증·임포트·nearby 등)이다. 실제로 A파트가 가져갈 "선점된 조회 계층"은 아래 §2의 약 500줄이다.

## 2. A파트로 넘기는 파일 (자유롭게 재작성 가능)

이 파일들은 A파트의 조회 도메인이다. **재작성해도 내 기능(검증·임포트·nearby·describe)과 충돌하지 않는다** — 단 §3의 계약만 지켜주면 된다.

| 파일 | 넘기는 이유 | 주의 |
|---|---|---|
| `content/controller/ContentController.java` | 작품 목록/상세/스팟 조회 라우팅 — 내가 깐 골격 | `route-verification` 엔드포인트만 내 것(§4) |
| `content/service/ContentService#findContents/findContent` | 목록·상세 조회 로직 | coco가 이미 목록 쿼리 구현함 |
| `content/dto/response/ContentListResponse.java` | 작품 목록 응답 | |
| `content/dto/response/ContentDetailResponse.java` | 작품 상세 응답 | |
| `content/dto/response/ContentSpotsResponse.java` | 작품별 스팟 응답 | **05 명세와 필드 드리프트 있음 → §5** |
| `spot/controller/SpotController#spot` | 성지 상세 조회 `GET /api/spots/{id}` | 같은 컨트롤러의 nearby·street-view는 내 것(§4) |
| `spot/service/SpotService#findSpot` | 성지 상세 조회 로직 | |
| `spot/dto/response/SpotDetailResponse.java` | 성지 상세 응답 | |
| `spot/dto/request/SpotReportCreateRequest`·`dto/response/SpotReportResponse`·`SpotService#createReport` | 성지 제보(F-5) — 현재 스텁 | 실저장 미구현(접수 응답만) |

## 3. ⚠️ 얽힘 주의 — `ContentService`·`ContentSpotsResponse`는 통째 재작성 금지

`ContentService`는 조회 API(A파트)와 **내 AI describe 기능이 한 클래스에 얽혀** 있다. 재작성 시 아래 계약을 반드시 유지해야 내 쪽(일정 생성·카드 표시)이 안 깨진다:

- `findContentSpots(contentId)` — 이 메서드는 **AI describe 결과를 병합**해 `koreanName`·`sceneDescription`·`specialPoint`를 채운다. 반환 DTO(`ContentSpotsResponse.SpotSummary`)의 이 필드들을 **없애면 안 됨**(프론트 카드·일정이 소비).
- `cachedRecommendedMinutes(spotId)` — **일정 생성(B파트)이 stayMinutes로 사용.** 시그니처 유지 필수.
- `prewarmDescriptions(contentId)` — 임포트 직후 @Async 프리웜. `AiDescribeClient` 의존.
- → **권장**: 조회 로직만 재작성하고, describe 병합/프리웜은 별도 협업 컴포넌트로 분리하거나 그대로 위임. 통짜 교체 ❌.

## 4. 내 기능이 spot/ 패키지에 두는 의존성 (A파트가 건드리면 내 쪽 깨짐)

이 파일들은 **위치만 spot/일 뿐 B파트/AI 기능**이다. A파트 재작성 범위에서 제외:

- **블로그 검증**: `RouteVerificationService`·`NaverBlogClient`·`BlogPostFetcher`·`AiRouteVerifyClient`·`RouteVerificationResponse` + `ContentController#routeVerification`
- **Anitabi 임포트**: `SpotImportService`·`AnitabiClient`·`AnitabiPoint`·`AnitabiWork`·`SpotImportResponse` + `admin/AdminSpotImportController`
- **AI 설명**: `AiDescribeClient`·`AiDescribeRequest`·`AiDescribeResult`·`SpotDescribePrewarmer`
- **주변장소**: `GooglePlacesProvider`·`NearbyAttractionsProvider`·`NearbyAttractionsResponse` + `SpotService#findNearby*`
- **지오코딩**: `GoogleReverseGeocoder`·`ReverseGeocoder`(임포트·출발지 앵커가 사용)
- **StreetView**: `StreetViewClient` + `SpotService#streetView`

`SpotController`·`SpotService`는 **A파트(상세) + B파트(nearby·street-view)가 공존**하는 파일이다. A파트는 `spot`/`findSpot`만 손대고 나머지 메서드는 두는 걸 권장.

## 5. 살아있는 기술부채 (넘기기 전 정리 필요)

1. **05 명세 ↔ 코드 드리프트**: `ContentSpotsResponse.SpotSummary`에 `koreanName·sceneDescription·specialPoint·sceneImageUrl`, 작품목록에 `spotCount`가 추가됐으나 `기획/05_API_명세.md`에 미반영. → 문서 갱신 필요(내가 유발, 내가 정리 가능).
2. **Flyway 마이그레이션 누락**: `visit_record`·`ai_request_log` 테이블이 엔티티만 있고 `V6`/`V7` 마이그레이션이 없다. 로컬 `ddl-auto: create-drop`이라 지금은 돌지만, **A파트가 `validate`로 전환 시 검증 실패**. → 넘기기 전에 내가 `V6__visit_record.sql`·`V7__ai_request_log.sql`을 기존 V1~V5 컨벤션대로 작성해야 함.

## 6. 권장 인수 절차

1. A파트가 §2 파일 목록 확인 → 재작성 범위 합의
2. B파트(나)가 먼저: §5-2 마이그레이션 2개 작성 + §5-1 문서 갱신 PR
3. A파트가 §2 조회 계층 재작성 시 §3 계약 준수 (describe 병합·cachedRecommendedMinutes·prewarm 유지)
4. §4 파일은 건드리지 않음 — 필요 시 나에게 인터페이스 요청
