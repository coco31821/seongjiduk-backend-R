# TDD 작업 가이드

성지덕 백엔드는 도메인별 패키지 구조를 기준으로 테스트를 먼저 작성합니다.

## 기본 흐름

1. `src/test/java/com/sungjiduk/backend/{domain}` 아래에 실패하는 테스트를 먼저 작성합니다.
2. 테스트 의도를 만족하는 최소 구현을 `src/main/java/com/sungjiduk/backend/{domain}` 아래에 추가합니다.
3. 테스트 통과 후 중복, 이름, 책임 분리를 정리합니다.
4. API 명세가 바뀌면 문서 repo의 SSOT를 먼저 수정하고 구현에 반영합니다.

## 테스트 위치

```text
src/test/java/com/sungjiduk/backend/{domain}/controller
src/test/java/com/sungjiduk/backend/{domain}/service
src/test/java/com/sungjiduk/backend/{domain}/repository
src/test/resources/fixtures/{domain}
```

## 구현 위치

```text
src/main/java/com/sungjiduk/backend/{domain}/controller
src/main/java/com/sungjiduk/backend/{domain}/service
src/main/java/com/sungjiduk/backend/{domain}/repository
src/main/java/com/sungjiduk/backend/{domain}/entity
src/main/java/com/sungjiduk/backend/{domain}/dto/request
src/main/java/com/sungjiduk/backend/{domain}/dto/response
src/main/java/com/sungjiduk/backend/{domain}/exception
src/main/java/com/sungjiduk/backend/{domain}/mapper
```

## 도메인 기준

- `auth`: 회원가입, 로그인, 토큰, 현재 사용자
- `user`: 사용자 프로필, 덕질 취향, 개인화 입력
- `content`: 작품, 작품별 성지 묶음
- `spot`: 성지/관광지 장소 데이터, 제보
- `trip`: 여행 조건 입력, AI 일정 생성, 일정 저장/공유
- `visit`: 방문 인증, 인증샷, 사용자 피드백
- `booking`: 항공권/숙소 외부 링크 추천
- `admin`: 관리자 통계, 운영 데이터 조회
- `event`: 접속/이용 이벤트 수집
- `seed`: MVP 시드 데이터 적재

## 이름 규칙

- Controller: `{Domain}Controller`
- Service: `{Domain}Service`
- Repository: `{Entity}Repository`
- Entity: 단수 명사 사용
- Request DTO: `{Action}Request`, 예: `TripCreateRequest`
- Response DTO: `{Target}Response`, 예: `TripDetailResponse`

## 커밋 규칙

커밋/PR 제목은 대문자 타입을 사용합니다.

```text
[FEAT] 여행 일정 생성 API 추가
[FIX] 로그인 검증 오류 수정
[TEST] 여행 일정 서비스 테스트 추가
[INFRA] 도메인별 TDD 디렉터리 구조 추가
[DOCS] API 명세 업데이트
```
