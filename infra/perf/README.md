# Redis 성능 실험 환경

외부 API key 없이 동일 조건에서 Redis 전후를 비교한다.

```bash
docker compose -f infra/perf/docker-compose.perf.yml up --build
```

- API: `http://localhost:8080`
- Prometheus: `http://localhost:9090`
- Grafana: `http://localhost:3000`

mock API는 외부 호출 200ms, AI 1,000ms, AI max in-flight 4로 고정한다. 각 실험 전 DB seed를 동일하게 준비하고 Redis에는 실험 prefix만 삭제한다. `CACHE_MODE`, `RATE_LIMIT_MODE`, `CONCURRENCY_LIMIT_MODE`를 `off`, `local`, `redis`로 바꿔 대조한다.

compose 기동 뒤 아래 SQL을 넣으면 `CONTENT_ID=1`로 실행할 수 있다.

```bash
Get-Content infra/perf/seed-perf.sql | docker compose -f infra/perf/docker-compose.perf.yml exec -T mysql mysql -useongjiduk -pseongjiduk-perf-password seongjiduk
```

## 2단계: 기준선 실행

초기 데이터는 compose 기동 후 기존 데모 seed를 실행해 준비한다. Anitabi API를 호출하지 않으려면 `scripts/seed/*.sql`의 정적 작품·spot 데이터를 MySQL에 넣고 `CONTENT_ID`를 확인한다.

```bash
# 30~60초 warm-up 뒤 3분 steady run. k6 설치 후 실행한다.
k6 run -e BASE_URL=http://localhost:8080 -e CONTENT_ID=1 -e RPS=5 infra/perf/k6/route-cache-baseline.js
```

결과마다 k6 JSON, `/debug/stats` 결과, Prometheus snapshot, Redis INFO를 `results/<run-id>/`에 함께 저장한다. 기준선은 현재 코드의 `REDIS_GUARD_MODE`만 사용하며, 이후 cache mode 분리 리팩토링 후 동일 스크립트로 다시 실행한다.
