# 인증/로그인 API (`/api/auth`)

모든 엔드포인트는 인증이 필요 없습니다(public). 카카오 OAuth 기반 로그인입니다.

> **참고**: 카카오 연동이 필요한 성공 경로(`login`, `kakao/callback`, `login/token`)는
> 로컬 테스트 환경에 실제 카카오 인가 코드/토큰이 없어 **성공 응답을 실제로 캡처하지 못했습니다**.
> 성공 응답은 소스의 DTO(`LoginResponse`, `TokenRefreshResponse`)로부터 도출한 계약(contract)을
> 기술하고, 실제 캡처한 **실패 응답**을 함께 제공합니다.

---

## GET `/api/auth/login/url`

카카오 로그인 인가(authorize) URL을 조회합니다.

- **인증**: 불필요

### 성공 (200)

```json
{
  "authUrl": "https://kauth.kakao.com/oauth/authorize/?client_id=1fa8b12793a1c39ea5644631201edb14&redirect_uri=http://localhost:8083/api/auth/kakao/callback&response_type=code"
}
```

---

## POST `/api/auth/login`

카카오 인가 코드(`code`)로 로그인합니다.

- **인증**: 불필요
- **Request Body**: `{ "code": "<카카오 인가 코드>" }` (`code` 필수)

### 성공 (200) — 계약(contract, 미실행)

`LoginResponse` 형태:

```json
{
  "tokenType": "Bearer",
  "accessToken": "<JWT access token>",
  "expiresIn": 86400,
  "refreshToken": "<refresh token>",
  "refreshTokenExpiresIn": 1209600,
  "kakaoId": "<카카오 회원번호>",
  "connectedAt": "<카카오 연결 시각>",
  "email": "<카카오 계정 이메일>",
  "isNewMember": false,
  "memberId": "<회원 id>"
}
```

> 실제 카카오 인가 코드가 필요해 성공 호출은 실행하지 못했습니다.

### 실패

| 상황 | HTTP | 응답 |
|---|---|---|
| 유효하지 않은 인가 코드 | 400 | `{"code":"E40004","message":"유효하지 않은 인가 코드입니다","timestamp":"..."}` |
| `code` 누락(검증) | 400 | `{"code":"E40001","message":"입력값이 올바르지 않습니다","errors":[{"field":"code","value":"","reason":"인가 코드 필수값이 누락되었습니다"}],"timestamp":"..."}` |

---

## GET `/api/auth/kakao/callback?code=<code>`

카카오 redirect 콜백. 내부적으로 `login(code)`과 동일하게 동작합니다.

- **인증**: 불필요
- **Query Param**: `code` (카카오 인가 코드)

### 성공 (200) — 계약(미실행)

`POST /api/auth/login`과 동일한 `LoginResponse`.

### 실패

| 상황 | HTTP | 응답 |
|---|---|---|
| 유효하지 않은 인가 코드 | 400 | `{"code":"E40004","message":"유효하지 않은 인가 코드입니다","timestamp":"..."}` |
| `code` 쿼리 파라미터 누락 | 500 | `{"code":"E50001","message":"서버 내부 오류가 발생했습니다","timestamp":"..."}` |

> 주의: 필수 쿼리 파라미터 누락(MissingServletRequestParameterException)은 전용 핸들러가 없어
> 일반 `Exception` 핸들러로 처리되어 **500**으로 응답합니다(400이 아님).

---

## POST `/api/auth/refresh`

Refresh Token으로 access token을 재발급합니다.

- **인증**: 불필요(바디의 refreshToken으로 검증)
- **Request Body**: `{ "refreshToken": "<refresh token>" }` (필수)

### 성공 (200) — 계약(미실행)

`TokenRefreshResponse` 형태:

```json
{
  "memberId": "<회원 id>",
  "kakaoId": "<카카오 회원번호>",
  "email": "<이메일>",
  "tokenType": "Bearer",
  "accessToken": "<새 access token>",
  "expiresIn": 86400,
  "refreshToken": "<refresh token>",
  "refreshTokenExpiresIn": 1209600,
  "connectedAt": "<연결 시각>",
  "isNewMember": false
}
```

### 실패

| 상황 | HTTP | 응답 |
|---|---|---|
| 유효하지 않은 refresh token | 401 | `{"code":"E40101","message":"유효하지 않은 Refresh Token입니다","timestamp":"..."}` |
| `refreshToken` 누락(검증) | 400 | `{"code":"E40001","message":"입력값이 올바르지 않습니다","errors":[{"field":"refreshToken","value":"","reason":"Refresh Token은 필수입니다"}],"timestamp":"..."}` |

---

## POST `/api/auth/login/token`

카카오 SDK가 발급한 access token으로 로그인합니다(앱 클라이언트용).

- **인증**: 불필요
- **Request Body**: `{ "accessToken": "<카카오 access token>" }`

### 성공 (200) — 계약(미실행)

`POST /api/auth/login`과 동일한 `LoginResponse`.

### 실패

| 상황 | HTTP | 응답 |
|---|---|---|
| 유효하지 않은 카카오 토큰 | 401 | `{"code":"E40101","message":"유효하지 않은 카카오 토큰입니다","timestamp":"..."}` |
