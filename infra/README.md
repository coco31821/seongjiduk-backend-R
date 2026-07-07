# infra

Docker Compose, cloud 배포, monitoring 설정을 관리합니다.

AI 서비스는 별도 repo `sungjiduk/seongjiduk-ai`에서 관리합니다. 로컬 compose는 기본적으로 sibling checkout `../seongjiduk-ai`를 build context로 사용합니다.

## Local

```bash
cp .env.example .env
docker compose -f infra/docker/docker-compose.local.yml --env-file .env up --build
```

Endpoints:

- Backend health: `http://localhost:8080/api/health`
- Swagger: `http://localhost:8080/swagger-ui.html`
- AI health: `http://localhost:8000/health`

## Production

```bash
cp infra/docker/.env.prod.example infra/docker/.env   # 값 채우기 (커밋 금지)
docker compose -f infra/docker/docker-compose.prod.yml --env-file infra/docker/.env up -d
```

구성: Caddy(HTTPS 자동, `/api·swagger·actuator`→backend, 나머지→frontend SPA) + backend + ai-service + frontend + MySQL + **Redis**(JWT 리프레시 저장) + Prometheus + Grafana(데이터소스 자동 프로비저닝).

필수 환경변수 (`infra/docker/.env.prod.example` 참고):

| 변수 | 설명 |
|---|---|
| `DOMAIN` | Caddy 도메인 (로컬 검증은 `:80`) |
| `MYSQL_DATABASE/USER/PASSWORD/ROOT_PASSWORD` | DB 접속 |
| `JWT_SECRET` | 32바이트 이상 |
| `OPENAI_API_KEY` | **비우면 ai-service가 mock으로 동작** (키 없이도 전체 스택 기동 가능) |
| `AI_MODEL` | 기본 `gpt-4o` (ADR-0004) |
| `GOOGLE_MAPS_API_KEY` | 선택 — 임포트 역지오코딩 품질 (없으면 city 폴백) |
| `GRAFANA_ADMIN_PASSWORD` | Grafana 관리자 |

이미지는 `ghcr.io/sungjiduk/seongjiduk-{backend,ai,frontend}:latest` 기본, `*_IMAGE` 변수로 오버라이드.

## 이미지 발행 (CI/CD)

각 repo의 `.github/workflows/deploy.yml`이 **dev/main push 시 GHCR로 이미지 push** (`:latest` + `:{sha}`). 별도 시크릿 불필요(`GITHUB_TOKEN` packages:write 사용).

서버 배포 절차(EC2 등):
1. 서버에 docker/compose 설치, 이 repo의 `infra/docker/` 복사(또는 sparse checkout)
2. `.env` 작성 → `docker compose -f docker-compose.prod.yml pull && docker compose -f docker-compose.prod.yml up -d`
3. 갱신 시 `pull` + `up -d`만 반복 (추후 GitHub Actions SSH 자동화 예정 — `DEPLOY_HOST/DEPLOY_KEY` 시크릿 필요)

⚠️ **알려진 선행 과제**: dev가 `ddl-auto: create-drop` 상태라 컨테이너 재시작 시 데이터가 초기화된다. **A파트 Flyway 마이그레이션 + `validate` 복원이 프로덕션 배포의 하드 블로커** (트러블슈팅 2026-07-07 참고).
