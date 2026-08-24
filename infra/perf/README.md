# Redis 성능 실험 환경

외부 API key 없이 동일 조건에서 Redis 전후를 비교한다.

```bash
docker compose -f infra/perf/docker-compose.perf.yml up --build
```

- API: `http://localhost:8080`
- Prometheus: `http://localhost:9090`
- Grafana: `http://localhost:3000`

mock API는 외부 호출 200ms, AI 1,000ms, AI max in-flight 4로 고정한다. 각 실험 전 DB seed를 동일하게 준비하고 Redis에는 실험 prefix만 삭제한다. `CACHE_MODE`, `RATE_LIMIT_MODE`, `CONCURRENCY_LIMIT_MODE`를 `off`, `local`, `redis`로 바꿔 대조한다.
