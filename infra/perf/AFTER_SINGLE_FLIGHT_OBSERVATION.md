# Route Verification — Redis single-flight 적용 후 관찰

실행일: 2026-08-25

## 동일 조건

- backend 2 replicas, MySQL 1, Redis 1
- AI mock: 1,000ms, capacity 4
- Naver/본문 mock: 200ms
- `GET /api/contents/1/route-verification`
- k6 constant-arrival-rate 5 RPS, 60초
- `CACHE_MODE=redis`, cache와 mock counters를 실행 전 초기화

## 결과

| 지표 | PR1 기준선 | single-flight 적용 후 |
| --- | ---: | ---: |
| 완료 요청 처리량 | 2.19 RPS | 5.02 RPS |
| p50 | 25,356ms | 7.98ms |
| p95 | 39,499ms | 710.01ms |
| 오류율 | 10.66% | 0% |
| dropped iterations | 85 | 0 |
| 완료 요청 | - | 301 |
| AI route verification 호출 | 195 | 1 |
| Naver search 호출 | 390 | 2 |
| 본문 호출 | 195 | 1 |
| AI max in-flight | 2 | 1 |

## 결론

동일 `contentId`의 최초 cache miss에서 Redis distributed single-flight가 한 요청만
Naver 검색(쿼리 2개), 본문 조회, route AI 검증을 수행하게 했다. 나머지 요청은 Redis
cache 값을 재사용해 외부 호출이 요청 수에 비례하던 cache stampede를 제거했다.

결과 JSON: `infra/perf/results/after-route-r5.json`
