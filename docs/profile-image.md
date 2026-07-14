# 프로필 이미지 API (`/api/members/me/profile-image`)

인증된 본인의 프로필 이미지를 등록/수정/삭제/조회합니다.
모두 `Authorization: Bearer <accessToken>` 헤더가 필요하며, 없으면 **403(빈 본문)** 입니다.
POST/PUT은 `multipart/form-data`, 파일 파트 이름은 `file`입니다.

응답 DTO(`ProfileImageResponse`): `{ "imageUrl": "...", "default": <boolean> }`
(`isDefault` 필드가 JSON에서는 `default`로 직렬화됩니다.)

> POST/PUT/DELETE는 회원의 이미지를 실제로 변경합니다. 아래 응답은 member 1로 실제 실행 후
> 시드 상태(image_id=1, mock-0001.png)로 DB를 복구하여 캡처한 것입니다.

---

## GET `/api/members/me/profile-image`

현재 프로필 이미지를 조회합니다(안전한 읽기).

- **인증**: 필요

### 성공 (200) — member 1

```json
{ "imageUrl": "http://localhost:8083/images/profile/mock-0001.png", "default": false }
```

커스텀 이미지가 없으면(기본 이미지) `default: true`, `imageUrl`은 `.../default.png`.

### 실패

| 상황 | HTTP | 응답 |
|---|---|---|
| 토큰 없음 | 403 | (빈 본문) |

---

## POST `/api/members/me/profile-image`

프로필 이미지를 등록(또는 교체)합니다.

- **인증**: 필요
- **Content-Type**: `multipart/form-data`, 파트 `file` (이미지 파일)

### 성공 (201)

```json
{ "imageUrl": "http://localhost:8083/images/profile/634bd480-01a5-4abf-81e2-e20a7b7ffb3e.png", "default": false }
```

### 실패

| 상황 | HTTP | 응답 |
|---|---|---|
| 이미지가 아닌 파일(text 등) | 400 | `{"code":"E40011","message":"이미지 파일만 업로드할 수 있습니다","timestamp":"..."}` |
| 토큰 없음 | 403 | (빈 본문) |

---

## PUT `/api/members/me/profile-image`

프로필 이미지를 수정합니다.

- **인증**: 필요
- **Content-Type**: `multipart/form-data`, 파트 `file`

### 성공 (200)

```json
{ "imageUrl": "http://localhost:8083/images/profile/2748742e-0dd0-4356-addf-9cd9994d3fc0.png", "default": false }
```

### 실패

| 상황 | HTTP | 응답 |
|---|---|---|
| 이미지가 아닌 파일 | 400 | `{"code":"E40011","message":"이미지 파일만 업로드할 수 있습니다","timestamp":"..."}` |
| 토큰 없음 | 403 | (빈 본문) |

---

## DELETE `/api/members/me/profile-image`

프로필 이미지를 삭제하고 기본 이미지로 복귀합니다.

- **인증**: 필요

### 성공 (200)

```json
{ "imageUrl": "http://localhost:8083/images/profile/default.png", "default": true }
```

### 실패

| 상황 | HTTP | 응답 |
|---|---|---|
| 토큰 없음 | 403 | (빈 본문) |
