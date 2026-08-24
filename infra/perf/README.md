# Redis 성능 실험 환경

외부 API key 없이 동일 조건에서 Redis 전후를 비교한다.

```bash
docker compose -f infra/perf/docker-compose.perf.yml up --build
```

- API: `http://localhost:8080`
- Prometheus: `http://localhost:9090`
- Grafana: `http://localhost:3000`

mock API는 외부 호출 200ms, AI 1,000ms, AI max in-flight 4로 고정한다. 각 실험 전 DB seed를 동일하게 준비하고 route verification Redis key와 mock 호출 카운터를 초기화한다.

이 PR의 기준선은 기존 `REDIS_GUARD_MODE=redis` 설정을 그대로 사용한다. `CACHE_MODE=off|local|redis`를 이용한 캐시 구현 대조는 아직 연결되지 않았으며, 다음 Route cache mode 리팩토링 PR에서 추가한다. 현재 `CACHE_MODE` 환경변수는 k6 결과 태그 용도일 뿐 캐시 동작을 바꾸지 않는다.

compose 기동 뒤 아래 SQL을 넣으면 `CONTENT_ID=1`로 실행할 수 있다.

```bash
Get-Content infra/perf/seed-perf.sql | docker compose -f infra/perf/docker-compose.perf.yml exec -T mysql mysql -useongjiduk -pseongjiduk-perf-password seongjiduk
```

## 2단계: Cold / Warm 기준선 실행

초기 데이터는 compose 기동 후 기존 데모 seed를 실행해 준비한다. Anitabi API를 호출하지 않으려면 `scripts/seed/*.sql`의 정적 작품·spot 데이터를 MySQL에 넣고 `CONTENT_ID`를 확인한다.

Cold-start burst와 warm-cache steady run은 반드시 분리한다. 두 상태를 하나의 결과에 섞으면 cache stampede 개선 효과와 cache-hit 성능을 구분할 수 없다.

```powershell
# 1) Cold-start: route cache와 mock 카운터를 함께 초기화한다.
.\infra\perf\scripts\reset-perf.ps1
k6 run -e BASE_URL=http://localhost:8080 -e CONTENT_ID=1 -e RPS=5 -e DURATION=1m --summary-export infra/perf/results/cold.json infra/perf/k6/route-cache-baseline.js

# 2) Warm-cache: 한 번만 prime 한 뒤, 캐시는 보존하고 mock 카운터만 초기화한다.
Invoke-WebRequest http://localhost:8080/api/contents/1/route-verification
.\infra\perf\scripts\reset-perf.ps1 -KeepRouteCache
k6 run -e BASE_URL=http://localhost:8080 -e CONTENT_ID=1 -e RPS=5 -e DURATION=1m --summary-export infra/perf/results/warm.json infra/perf/k6/route-cache-baseline.js

# 3) 각 실행 직후 외부 호출 수를 저장한다.
Invoke-RestMethod http://localhost:8081/debug/stats | ConvertTo-Json -Depth 5
```

결과마다 k6 JSON, `/debug/stats` 결과, Prometheus snapshot, Redis INFO를 `results/<run-id>/`에 함께 저장한다. 결과 파일에는 backend commit SHA, Java/k6 버전, Docker image digest, host CPU/RAM도 기록한다. 기준선은 현재 코드의 `REDIS_GUARD_MODE`만 사용하며, 이후 cache mode 분리 리팩토링 후 동일 스크립트로 다시 실행한다.
