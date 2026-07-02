# seongjiduk-backend

성지덕 백엔드 레포입니다.

## 역할

- Spring Boot REST API
- Spring Security 인증/인가
- Spring Data JPA 기반 영속화
- 관리자 통계 API
- LangGraph AI 서비스 연동 HTTP client
- Docker Compose, 배포/모니터링 인프라

## 관련 레포

- 문서/기획 SSOT: https://github.com/sungjiduk/seongjiduk
- 프론트엔드: https://github.com/sungjiduk/seongjiduk-frontend
- AI 서비스: https://github.com/sungjiduk/seongjiduk-ai

## 디렉토리 구조

```text
.
├── src/main/java/com/sungjiduk/backend
│   ├── auth/         # 회원가입, 로그인, 토큰
│   ├── user/         # 사용자 프로필, 덕질 취향
│   ├── content/      # 작품, 콘텐츠 메타데이터
│   ├── spot/         # 성지/관광지 장소 데이터
│   ├── trip/         # AI 여행 일정 생성, 저장, 공유
│   ├── visit/        # 방문 인증, 피드백
│   ├── booking/      # 항공권/숙소 외부 링크 추천
│   ├── admin/        # 관리자 통계
│   ├── event/        # 접속/이용 이벤트
│   ├── seed/         # MVP 시드 데이터
│   └── common/       # 공통 응답, 설정, 예외, 보안
├── src/test/java/com/sungjiduk/backend
│   ├── {domain}/controller
│   ├── {domain}/service
│   ├── {domain}/repository
│   └── support/
├── src/test/resources/fixtures
├── docs/             # 백엔드 작업 규칙
├── infra/            # Docker, cloud, monitoring
├── .github/          # GitHub Actions, PR/Issue templates
├── Dockerfile
├── build.gradle
├── settings.gradle
└── README.md
```

각 도메인은 기본적으로 `controller`, `service`, `repository`, `entity`, `dto/request`, `dto/response`, `exception`, `mapper` 패키지를 가집니다.

## 빠른 시작

```bash
cp .env.example .env
./gradlew test
docker compose -f infra/docker/docker-compose.local.yml --env-file .env up --build
```

로컬 Docker Compose는 sibling 경로의 `../seongjiduk-ai` repo를 AI 서비스 build context로 사용합니다.

로컬 엔드포인트:

- Backend health: `http://localhost:8080/api/health`
- Swagger: `http://localhost:8080/swagger-ui.html`
- AI health: `http://localhost:8000/health`

## TDD 작업 방식

구현 전에 `docs/TDD_GUIDE.md`를 먼저 확인합니다.

1. `src/test/java/com/sungjiduk/backend/{domain}`에 실패하는 테스트를 먼저 작성합니다.
2. `src/main/java/com/sungjiduk/backend/{domain}`에 최소 구현을 추가합니다.
3. `./gradlew test`로 전체 테스트를 통과시킨 뒤 커밋합니다.

## API 호출 예시

기본 호출 형태는 `docs/API_CALL_EXAMPLES.http`에서 확인합니다.

현재 API는 병렬 개발 시작을 위한 mock 응답 기반 골격입니다. 실제 구현 시 각 도메인 `Service`의 mock 데이터를 `Repository` 호출과 비즈니스 로직으로 교체합니다.

초기 개발용 Basic Auth 계정:

- USER: `user` / `password1234`
- ADMIN: `admin` / `admin1234`

## CI/CD 상태

- `Backend CI`: Gradle test, backend Docker build
- AI 서비스 CI는 `sungjiduk/seongjiduk-ai` repo에서 별도로 수행
- CD 배포 workflow는 클라우드/서버 확정 후 추가

## 작업 전 확인

기능 구현 전에 문서 repo의 SSOT를 먼저 확인합니다.

1. `기획/02_최종_기획서.md`
2. `기획/03_기능_명세.md`
3. `기획/04_도메인_모델_ERD.md`
4. `기획/05_API_명세.md`
5. `기획/06_AI_에이전트_설계.md`
6. `기획/07_관리자_통계_설계.md`
