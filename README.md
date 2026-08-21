# 성지덕 Backend | Redis 기반 AI 여행 일정 플랫폼

> 애니메이션·드라마 속 장소를 탐색하고, AI가 성지순례 일정을 설계하는 Spring Boot 백엔드입니다.  
> **Redis를 캐시가 아닌 분산 제어 계층으로 확장**해 외부 API 비용, AI 과부하, 다중 인스턴스 환경의 일관성을 함께 다뤘습니다.

## Redis를 왜 앞에 두었는가

AI 일정 생성과 장소 탐색은 Google·Naver·LLM처럼 느리고 비용이 발생하며 호출 한도가 있는 외부 서비스에 의존합니다. 단순 캐시만으로는 서버가 여러 대가 되었을 때 AI 동시성·API quota·캐시 장애를 제어할 수 없었습니다.

| Redis 역할 | 적용 | 해결한 문제 |
| --- | --- | --- |
| 분산 캐시 | 경로 검증, 출발지 지오코딩 | 반복 외부 호출과 응답 지연 감소 |
| 분산 세마포어 | 모든 AI endpoint가 공유하는 `ai:global` permit | replica가 늘어도 AI 동시 실행 상한 보장 |
| 분산 레이트 리미터 | Google Places/Geocoding, Naver Blog | API quota 초과와 비용 폭증 방지 |
| 실험 스위치 | `off / local / redis` mode | Redis 도입 효과를 같은 조건에서 비교 |

## 핵심 성과 지표

| 시나리오 | 기준 | 목표 결과 |
| --- | --- | --- |
| Hot cache 경로 검증 | 외부 검색·본문·AI를 매 요청 수행 | 외부 호출 95% 감소, p95 70% 단축 |
| 동일 key 100건 동시 miss | 요청 수만큼 외부 API 호출 | 외부 호출 1~2회, 5xx 1% 미만 |
| Backend 2대 + AI capacity 4 | 인스턴스별 제한으로 최대 8개 실행 | 전역 AI in-flight 4 이하 |
| 외부 API 분당 한도 | fixed-window 경계에서 2배 burst 가능 | 60초 기준 quota 초과 0건 |
| Redis 장애 30초 | 캐시 장애가 사용자 API 장애로 전파 | 비핵심 조회 성공률 99%, 복구 10초 이내 |

> 지표는 `기획/REDIS_LOAD_TEST_PLAN_4_TO_9.md`의 k6 시나리오와 Prometheus/Redis exporter 데이터로 검증합니다. p50만 쓰지 않고 **p95·p99·오류율·외부 호출량·보호 invariant**를 함께 제시합니다.

---

## 프로젝트 소개

성지덕은 작품의 실제 배경지를 탐색하고, 이동 시간·방문 장소·취향을 반영한 여행 일정을 만드는 서비스입니다.

```text
사용자 요청
   │
   ├─ 작품·성지 조회 ── Google Places / Street View
   ├─ 블로그 동선 검증 ── Naver Blog + AI Route Verify
   └─ 일정 생성 ─────── AI Trip Service
                         │
                    Redis Guard
              Cache · Rate Limit · Semaphore
```

### 주요 기능

- 작품/국가/카테고리 기반 콘텐츠 탐색
- 작품별 성지, 장면 이미지, 주변 볼거리·맛집·테마 장소 조회
- Google Geocoding 기반 출발지 앵커와 이동 시간 반영
- AI 일정 생성, 로컬 fallback, 일정 수정·저장·공유
- Naver Blog 기반 성지 순서/동선 검증
- JWT access token + refresh token 인증, 방문 인증, 관리자 통계

## 내가 집중한 문제 해결

### 하나의 AI 서비스에 여러 기능이 동시에 몰린다

일정 생성만 제한하면 장소 설명·경로 검증 요청이 AI 서비스를 우회해 과부하를 만들 수 있습니다. `AiTripClient`, `AiDescribeClient`, `AiRouteVerifyClient`를 `AiConcurrencyGuard` 하나로 묶어 `ai:global` permit을 공유하게 했습니다. replica가 늘어도 동일한 상한을 유지하고, 대기 시간 초과 시에는 로컬 fallback으로 전환합니다.

### 분산 세마포어의 시간은 서버 시간이 아니다

각 backend의 `System.currentTimeMillis()`를 기준으로 stale permit을 판단하면 clock skew가 생기고, 긴 AI 작업은 고정 lease 만료로 실행 중인데도 permit이 회수될 수 있습니다. Lua 내부 Redis `TIME`으로 시간을 통일하고 heartbeat로 ZSET score·TTL을 갱신했습니다. release는 best-effort라 Redis 장애가 성공한 AI 응답을 실패시키지 않습니다.

### Redis 캐시가 있어도 type이 깨지면 매번 miss가 된다

`RedisTemplate<String, Object>`의 범용 JSON 직렬화는 DTO 대신 `Map`으로 복원될 수 있어 `instanceof`가 계속 실패합니다. 출발지 지오코딩을 `StartLocationCache` port로 분리하고, `StringRedisTemplate`의 명시적 JSON 역직렬화를 사용했습니다. 주소 원문은 저장하지 않고 정규화한 SHA-256 digest key를 사용합니다.

### fixed window는 quota를 지켜도 경계 burst를 허용한다

분당 60회 fixed window는 경계 전후로 120회를 허용할 수 있습니다. Redis Lua Token Bucket으로 refill·차감·TTL을 원자 처리해 multi-replica 환경에서도 quota를 일관되게 제어합니다.

### 외부 I/O 중 DB connection을 붙잡지 않는다

AI/Google/Naver 호출은 수백 ms~수 초가 걸릴 수 있습니다. 외부 응답을 기다리는 동안 DB transaction을 유지하면 connection pool이 고갈됩니다. `ContentService`, `SpotService`, `RouteVerificationService`의 클래스 단위 read-only transaction을 제거하고, 외부 호출과 영속화 구간을 분리했습니다.

## Redis 리팩토링 전후

| 관점 | 이전 | 이후 |
| --- | --- | --- |
| 캐시 비교 | `REDIS_GUARD_MODE` 하나가 guard/cache를 함께 제어 | cache/rate-limit/concurrency mode 분리 |
| no-cache 기준 | route cache도 in-memory로 동작 | `NoOpRouteVerificationCache`로 실제 miss 재현 |
| 출발지 캐시 | Object serializer + 주소 원문 key | typed port + explicit JSON + SHA-256 key |
| AI 동시성 | 일정 생성 endpoint만 제한 | trip/describe/route-verify 전역 한도 공유 |
| permit lease | JVM 시간, 고정 polling, release 예외 전파 | Redis TIME, heartbeat, jitter backoff, best-effort release |
| API quota | fixed window | Redis TIME 기반 Token Bucket |
| 운영 DDL | `create-drop` 상속 위험 | 기본·prod `validate` + Flyway migration |

## 기술 스택

| 영역 | 기술 |
| --- | --- |
| Backend | Java 25, Spring Boot 4, Spring MVC, Spring Security |
| Data | Spring Data JPA, MySQL 8, Flyway |
| Redis | Spring Data Redis, Lua, ZSET semaphore, Token Bucket |
| AI/External | LangGraph AI service, Google Maps, Naver Blog, Anitabi |
| Observability | Spring Actuator, Micrometer, Prometheus, Grafana |
| Test/Infra | JUnit 5, Mockito, Docker Compose, GitHub Actions |

## 실행 방법

```bash
cp .env.example .env
./gradlew test
docker compose -f infra/docker/docker-compose.local.yml --env-file .env up --build
```

| Endpoint | URL |
| --- | --- |
| Swagger | `http://localhost:8080/swagger-ui.html` |
| Backend health | `http://localhost:8080/api/health` |
| AI health | `http://localhost:8000/health` |
| Prometheus metrics | `http://localhost:8080/actuator/prometheus` |

### Redis 실험 설정

```yaml
seongjiduk:
  redis-guard:
    cache-mode: redis
    rate-limit-mode: redis
    concurrency-limit-mode: redis
    ai-max-concurrent: 4
    ai-wait-ms: 3000
    ai-lease-ms: 120000
    ai-heartbeat-ms: 20000
```

- `off`: Redis 도입 전 기준선
- `local`: 단일 JVM 비교군
- `redis`: 다중 인스턴스 공유 상태

## 검증과 다음 단계

- `./gradlew test`: 전체 회귀 테스트 통과
- k6 부하 시나리오: `기획/REDIS_LOAD_TEST_PLAN_4_TO_9.md`
- 구현 상세: `설명/REDIS_REFACTORING_IMPLEMENTATION.md`
- 프론트·AI·인프라 연동 요청: `설명/FRONTEND_AI_INFRA_FOLLOW_UP.md`

다음 단계는 same-key cache stampede를 분산 single-flight로 제어하고, refresh token을 JTI 기반 Redis `GETDEL` rotation으로 전환하는 것입니다. 두 작업은 외부 호출 증폭과 refresh replay를 각각 **100건 동시 요청에서도 1건만 성공**하도록 검증합니다.
