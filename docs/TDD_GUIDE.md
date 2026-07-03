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

## 테스트 스타일 (BDD)

모든 테스트는 BDD 스펙 스타일로 작성합니다.

- 테스트 대상(메서드/기능)별로 `@Nested` 클래스로 그룹핑한다.
- 클래스/그룹/각 테스트에 한글 `@DisplayName`을 붙여 "무엇을 하면 무엇이 된다"를 문장으로 표현한다. (스펙 트리의 `it` 역할)
- 각 테스트 본문은 `// given` `// when` `// then` 3구간으로 나눈다.
- 단언은 AssertJ(`assertThat`, `assertThatThrownBy`)를 사용한다.
- 협력 객체를 목킹할 때는 BDDMockito(`given(...).willReturn(...)`, `then(...).should()`)를 쓴다.
- 테스트 메서드명은 camelCase 동작 요약(예: `savesDraftPlan`)으로 두고, 사람이 읽는 설명은 `@DisplayName`에 담는다.

```java
@SpringBootTest
@Transactional
@DisplayName("TripService")
class TripServiceTest {

    @Nested
    @DisplayName("generate는")
    class Generate {

        @Test
        @DisplayName("일정을 DRAFT 상태로 저장한다")
        void savesDraftPlan() {
            // given
            TripGenerateRequest request = ...;
            // when
            TripResponse response = tripService.generate(request);
            // then
            assertThat(response.tripId()).isNotNull();
        }
    }
}
```

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
