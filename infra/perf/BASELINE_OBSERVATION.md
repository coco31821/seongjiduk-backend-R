# Route Verification 기준선 관찰

실행일: 2026-08-25

## 조건

- backend 2 replicas, MySQL 1, Redis 1
- AI mock: 1,000ms, capacity 4
- Naver/본문 mock: 200ms
- `GET /api/contents/1/route-verification`
- k6 constant-arrival-rate 5 RPS, 60초

## 결과

| 지표 | 값 |
| --- | ---: |
| 완료 요청 처리량 | 2.19 RPS |
| p50 | 25,356ms |
| p95 | 39,499ms |
| 오류율 | 10.66% |
| dropped iterations | 85 |
| AI mock 호출 | 195 |
| Naver search 호출 | 390 |
| 본문 호출 | 195 |
| AI max in-flight | 2 |

## 원인 가설과 근거

동일 contentId임에도 초기 cache miss 동안 모든 요청이 `get → Naver 2회 → 본문 → AI → put`을 병렬 실행했다. 즉 cache stampede로 AI/외부 호출이 요청 수에 비례했다. 또한 `RouteVerificationService`의 클래스 단위 read-only transaction이 외부 I/O 동안 유지되어, 각 backend Hikari pool 10개가 모두 active가 되었고 `Connection is not available, request timed out after 30000ms` 오류가 발생했다.

다음 실험은 transaction 경계 분리와 distributed single-flight를 적용한 뒤 같은 script·seed·mock 지연으로 재실행한다.
