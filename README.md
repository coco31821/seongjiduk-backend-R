# seongjiduk-backend

성지덕 백엔드 레포입니다.

## 역할

- Spring Boot REST API
- Spring Security 인증/인가
- Spring Data JPA 기반 영속화
- 관리자 통계 API
- LangGraph AI 서비스 연동
- Docker Compose, 배포/모니터링 인프라

## 관련 레포

- 문서/기획 SSOT: https://github.com/sungjiduk/seongjiduk
- 프론트엔드: https://github.com/sungjiduk/seongjiduk-frontend

## 디렉토리 계획

```text
.
├── src/              # Spring Boot source
├── ai-service/       # LangGraph Python service
├── infra/            # docker, cloud, monitoring
├── .github/          # PR/Issue templates, GitHub Actions
├── Dockerfile        # backend image
├── build.gradle
├── settings.gradle
└── README.md
```

## 빠른 시작

```bash
cp .env.example .env
./gradlew test
docker compose -f infra/docker/docker-compose.local.yml --env-file .env up --build
```

로컬 엔드포인트:

- Backend health: `http://localhost:8080/api/health`
- Swagger: `http://localhost:8080/swagger-ui.html`
- AI health: `http://localhost:8000/health`

## CI/CD 상태

- `Backend CI`: Gradle test, backend Docker build, ai-service Docker build
- CD 배포 workflow는 클라우드/서버 확정 후 추가

## 작업 전 확인

기능 구현 전에 문서 repo의 SSOT를 먼저 확인합니다.

1. `기획/02_최종_기획서.md`
2. `기획/03_기능_명세.md`
3. `기획/04_도메인_모델_ERD.md`
4. `기획/05_API_명세.md`
5. `기획/06_AI_에이전트_설계.md`
6. `기획/07_관리자_통계_설계.md`
