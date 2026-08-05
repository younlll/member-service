# member-service REST API 문서

mub-project 회원 서비스(`member-service`)의 REST API 명세입니다.
모든 응답 예시는 로컬에서 실제로 호출하여 캡처한 결과입니다.

## 기본 정보

- **Base URL**: `http://localhost:8083`
- **Content-Type**: 별도 표기 없으면 `application/json`
- **프로필 active**: `docker`

## 인증 모델 (JWT Bearer, HS512)

- 토큰은 `Authorization: Bearer <accessToken>` 헤더로 전달합니다.
- 토큰의 `sub` 클레임이 `memberId`이며, 인증 컨텍스트에서 본인을 식별합니다.
- **인증 불필요(public)** 경로: `/api/auth/**`, `/api/districts/**`, `/api/interests`, `/api/internal/**`, `/actuator/**`
- 그 외 모든 경로는 유효한 토큰이 필요합니다.

### 인증 실패 동작 (중요)

토큰이 없거나 유효하지 않거나, 토큰은 유효하지만 회원이 `DELETED`/미존재인 경우
Spring Security 기본 동작에 따라 **HTTP 403**과 **빈 응답 본문**을 반환합니다.
(ErrorResponse JSON 형태가 아닙니다.) 이 문서에서 "403 (no token)"으로 표기된
케이스는 모두 본문이 비어 있습니다.

## 공통 에러 응답 형식

비즈니스/검증 오류는 다음 JSON 형태로 반환됩니다(`@RestControllerAdvice`).

```json
{
  "code": "E40001",
  "message": "입력값이 올바르지 않습니다",
  "errors": [
    { "field": "nickname", "value": "", "reason": "닉네임은 필수입니다" }
  ],
  "timestamp": "2026-06-26T05:48:44.152975382"
}
```

- `errors` 배열은 `@Valid` 바디 검증 실패(MethodArgumentNotValidException)에서만 포함되며,
  그 외에는 `@JsonInclude(NON_NULL)` 규칙으로 생략됩니다.

### 주요 ErrorCode

| code | HTTP | message |
|---|---|---|
| `E40001` | 400 | 입력값이 올바르지 않습니다 |
| `E40002` | 400 | 타입이 올바르지 않습니다 |
| `E40004` | 400 | 유효하지 않은 인가 코드입니다 |
| `E40005` | 400 | 이미 회원가입이 완료된 회원입니다 |
| `E40009` | 400 | 해당 관심사에 유효하지 않은 옵션입니다(동적 메시지) |
| `E40010` | 400 | 유효하지 않은 지역 코드입니다 |
| `E40011` | 400 | 유효하지 않은 이미지 파일입니다 |
| `E40101` | 401 | 유효하지 않은 토큰입니다(refresh/카카오 토큰) |
| `E40401` | 404 | 회원을 찾을 수 없습니다 |
| `E40404` | 404 | 멤버십 상품을 찾을 수 없습니다 |
| `E50001` | 500 | 서버 내부 오류가 발생했습니다 |

## 엔드포인트 그룹

| 그룹 | 파일 | 인증 | 비고 |
|---|---|---|---|
| 인증/로그인 (`/api/auth`) | [auth.md](auth.md) | public | Kakao 의존 |
| 회원 (`/api/members`) | [members.md](members.md) | 필요 | |
| 멤버십 (`/api/memberships`) | [membership.md](membership.md) | 필요 | IAP 구독(B1 상품조회) |
| 프로필 이미지 (`/api/members/me/profile-image`) | [profile-image.md](profile-image.md) | 필요 | multipart |
| 온보딩 (`/api/onboarding`) | [onboarding.md](onboarding.md) | 필요 | multipart |
| 지역 (`/api/districts`) | [districts.md](districts.md) | public | |
| 관심사 (`/api/interests`) | [interests.md](interests.md) | public | |
| 내부 호출 (`/api/internal/members`) | [internal.md](internal.md) | public(클러스터 내부) | |

## 관심사/옵션 enum (참고)

`interestType` 값: `SELF_DEVELOPMENT`, `INVESTMENT`, `READING`, `EDUCATION`,
`SPORTS`, `RESTAURANT`, `MUSIC`, `DANCE`, `PHOTO`.

관심사별 허용 옵션은 [interests.md](interests.md) / `GET /api/interests` 응답을 참고하세요.
타입에 허용되지 않은 옵션을 보내면 `E40009`가 발생합니다.
