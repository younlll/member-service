# 내부 호출 API (`/api/internal/members`)

서비스 간(service-to-service) 회원 조회용 엔드포인트입니다.
브라우저/모바일 클라이언트용이 아니며 클러스터 내부에서만 호출되어야 합니다
(게이트웨이/인그레스가 이 경로를 외부에 노출하면 안 됨). 인증 필터상 public 입니다.

---

## GET `/api/internal/members?ids=<id,id,...>`

여러 회원을 id로 한 번에 조회합니다. 다른 서비스(예: space-service)가
닉네임/이메일/프로필 이미지로 자신의 데이터를 보강할 때 사용합니다.
존재하지 않는 id는 조용히 제외됩니다.

- **인증**: 불필요(내부 전용)
- **Query Param**: `ids` (콤마 구분 또는 반복 파라미터로 전달하는 회원 id 목록)

### 성공 (200) — `ids=1,2,3`

```json
[
  {
    "memberId": 1,
    "nickname": "민준이",
    "profileImageUrl": "http://localhost:8083/images/profile/mock-0001.png",
    "email": "kim.minjun@kakao.com"
  },
  {
    "memberId": 2,
    "nickname": "소연",
    "profileImageUrl": "http://localhost:8083/images/profile/mock-0002.png",
    "email": "lee.soyeon@kakao.com"
  },
  {
    "memberId": 3,
    "nickname": "준호짱",
    "profileImageUrl": "http://localhost:8083/images/profile/mock-0003.png",
    "email": "park.junho@kakao.com"
  }
]
```

- `profileImageUrl`은 항상 채워집니다. 커스텀 이미지가 없는 회원은 기본 이미지 URL로 응답합니다.

### 성공 (200) — 커스텀 이미지 없는 회원 `ids=9`

```json
[
  {
    "memberId": 9,
    "nickname": "동현",
    "profileImageUrl": "http://localhost:8083/images/profile/default.png",
    "email": "oh.donghyun@kakao.com"
  }
]
```

### 실패

| 상황 | HTTP | 응답 |
|---|---|---|
| `ids` 파라미터 누락 | 500 | `{"code":"E50001","message":"서버 내부 오류가 발생했습니다","timestamp":"..."}` |

> 주의: 필수 쿼리 파라미터 누락은 전용 핸들러가 없어 일반 `Exception` 핸들러로 처리되어 **500**입니다(400 아님).
> 존재하지 않는 id가 포함되어도 오류 없이 해당 id만 결과에서 제외됩니다.
