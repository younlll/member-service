# 온보딩 API (`/api/onboarding`)

회원가입(온보딩) 완료 처리. `Authorization: Bearer <accessToken>` 헤더가 필요합니다.

---

## POST `/api/onboarding/complete`

`multipart/form-data`로 온보딩 입력(JSON `request` 파트)과 선택적 프로필 이미지(`profileImage` 파트)를
함께 받아 회원가입을 완료합니다. 약관 동의, 닉네임, 활동 지역, 관심사를 저장하고
회원 상태를 `ACTIVE`로 전환합니다.

- **인증**: 필요
- **Content-Type**: `multipart/form-data`
  - `request` (필수, `application/json`): 온보딩 입력
  - `profileImage` (선택, 이미지 파일): 없으면 기본 이미지
- **`request` JSON 형태**:

```json
{
  "termsAgreementRequest": {
    "termsOfService": true,
    "privacyPolicy": true,
    "locationService": true,
    "marketing": true
  },
  "nickname": "신규유저",
  "distCode1": "11",
  "distCode2": "11680",
  "interests": [
    { "interestType": "MUSIC", "options": ["SOUNDPROOF", "RECORDING"] },
    { "interestType": "SPORTS", "options": ["INDOOR"] }
  ]
}
```

제약:
- `termsAgreementRequest`의 `termsOfService`/`privacyPolicy`/`locationService`는
  `@AssertTrue` — `true`가 아니면 검증 실패. `marketing`은 선택(기본 false).
- `nickname` 필수·최대 10자·정규식 `^[가-힣a-zA-Z0-9]+$`.
- `distCode1`/`distCode2` 필수. `interests` 1~3개(필수). 각 `options`(선호 편의시설)는 **선택** — 미입력·0개 허용.

### 성공 (201) — member 11(INACTIVE)로 실제 실행

```json
{
  "memberId": 11,
  "nickname": "신규유저",
  "message": "회원가입이 완료되었습니다"
}
```

> member 11로 실제 실행하여 캡처했고, 이후 시드(INACTIVE·미온보딩) 상태로 DB를 복구했습니다.

### 실패

| 상황 | HTTP | 응답 |
|---|---|---|
| 이미 온보딩 완료한 회원(예: member 1) | 400 | `{"code":"E40005","message":"이미 회원가입이 완료된 회원입니다","timestamp":"..."}` |
| 필수 약관 미동의(`termsOfService:false`) | 400 | `{"code":"E40001",...,"errors":[{"field":"termsAgreementRequest.termsOfService","value":"false","reason":"서비스 이용약관에 동의해야 합니다"}]}` |
| 닉네임 공백 | 400 | `{"code":"E40001",...,"errors":[{"field":"nickname","value":"","reason":"닉네임은 필수입니다"},{"field":"nickname","value":"","reason":"닉네임은 한글, 영문, 숫자만 가능합니다"}]}` |
| 토큰 없음 | 403 | (빈 본문) |
