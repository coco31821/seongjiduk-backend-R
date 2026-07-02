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

## Production skeleton

```bash
docker compose -f infra/docker/docker-compose.prod.yml --env-file .env.production up -d
```

Production requires these environment variables:

- `DOMAIN`
- `POSTGRES_DB`
- `POSTGRES_USER`
- `POSTGRES_PASSWORD`
- `JWT_SECRET`
- `GRAFANA_ADMIN_PASSWORD`
- `BACKEND_IMAGE`
- `AI_IMAGE`
