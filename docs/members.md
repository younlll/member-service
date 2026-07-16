# 회원 API (`/api/members`)

별도 표기 없으면 `Authorization: Bearer <accessToken>` 헤더가 필요합니다.
토큰/회원이 유효하지 않으면 **403(빈 본문)** 입니다.

---

## GET `/api/members/me`

인증된 본인의 프로필을 조회합니다.

- **인증**: 필요

### 성공 (200) — member 1 토큰

```json
{
  "memberId": 1,
  "nickname": "민준이",
  "bio": null,
  "imageId": 1,
  "regionProvince": "서울특별시",
  "regionCity": "강남구",
  "status": "ACTIVE",
  "interests": []
}
```

### 실패

| 상황 | HTTP | 응답 |
|---|---|---|
| 토큰 없음 | 403 | (빈 본문) |
| 잘못된/만료 토큰 | 403 | (빈 본문) |

---

## PUT `/api/members/me/profile`

본인의 닉네임, 한줄소개, 활동 지역, 관심사를 수정합니다.

- **인증**: 필요
- **Request Body**:

```json
{
  "nickname": "민준이",
  "bio": "테스트 한줄소개",
  "distCode1": "11",
  "distCode2": "11680",
  "interests": [
    { "interestType": "SPORTS", "options": ["INDOOR", "SHOWER"] },
    { "interestType": "MUSIC", "options": ["SOUNDPROOF"] }
  ]
}
```

제약: `nickname` 필수·최대 10자·정규식 `^[가-힣a-zA-Z0-9]+$`, `bio` 최대 30자,
`distCode1`/`distCode2` 필수, `interests` 1~3개·각 항목 `options` 최소 1개.

### 성공 (200)

```json
{
  "memberId": 1,
  "nickname": "민준이",
  "bio": "테스트 한줄소개",
  "imageId": 1,
  "regionProvince": "서울특별시",
  "regionCity": "강남구",
  "status": "ACTIVE",
  "interests": [
    { "interestType": "SPORTS", "options": ["INDOOR", "SHOWER"] },
    { "interestType": "MUSIC", "options": ["SOUNDPROOF"] }
  ]
}
```

### 실패

| 상황 | HTTP | 응답 |
|---|---|---|
| 닉네임 공백 | 400 | `{"code":"E40001",...,"errors":[{"field":"nickname","value":"","reason":"닉네임은 한글, 영문, 숫자만 가능합니다"},{"field":"nickname","value":"","reason":"닉네임은 필수입니다"}]}` |
| 닉네임 11자(>10) | 400 | `{"code":"E40001",...,"errors":[{"field":"nickname","value":"12345678901","reason":"닉네임은 최대 10자까지 가능합니다"}]}` |
| 관심사 0개 | 400 | `{"code":"E40001",...,"errors":[{"field":"interests","value":"[]","reason":"관심사는 1개 이상 3개 이하로 선택해야 합니다"}]}` |
| 관심사 4개(>3) | 400 | `{"code":"E40001",...,"errors":[{"field":"interests",...,"reason":"관심사는 1개 이상 3개 이하로 선택해야 합니다"}]}` |
| 타입에 없는 옵션 | 400 | `{"code":"E40009","message":"'음악/악기' 관심사에는 '잔디구장' 옵션을 선택할 수 없습니다","timestamp":"..."}` |
| 유효하지 않은 지역 코드 | 400 | `{"code":"E40010","message":"유효하지 않은 지역 코드입니다","timestamp":"..."}` |
| 토큰 없음 | 403 | (빈 본문) |

---

## GET `/api/members?email=<email>`

이메일로 회원 정보를 조회합니다.

- **인증**: 필요
- **Query Param**: `email`

### 성공 (200) — `email=lee.soyeon@kakao.com`

```json
{
  "memberId": 2,
  "email": "lee.soyeon@kakao.com",
  "nickname": "소연",
  "snsProvider": "KAKAO",
  "socialId": "kakao_1002",
  "regionProvince": "서울특별시",
  "regionCity": "마포구",
  "status": "ACTIVE"
}
```

### 실패

| 상황 | HTTP | 응답 |
|---|---|---|
| 존재하지 않는 이메일 | 404 | `{"code":"E40401","message":"존재하지 않는 회원입니다.","timestamp":"..."}` |
| 토큰 없음 | 403 | (빈 본문) |

---

## DELETE `/api/members/me`

본인을 탈퇴 처리합니다(soft delete). 상태를 `DELETED`로 전환하고 리프레시 토큰을 무효화합니다.

- **인증**: 필요

### 성공 (204)

본문 없음(No Content). (member 11로 실제 실행 후 시드 상태로 복구함.)

### 실패

| 상황 | HTTP | 응답 |
|---|---|---|
| 토큰 없음 | 403 | (빈 본문) |
| 이미 탈퇴(DELETED)한 회원의 토큰 | 403 | (빈 본문) — JWT 필터가 DELETED 회원을 차단하므로, 탈퇴 후 같은 토큰의 모든 요청은 403 |

> 탈퇴 직후 동일 토큰으로 `GET /api/members/me`를 호출하면 403(빈 본문)이 됩니다.
