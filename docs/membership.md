# 멤버십 (`/api/memberships`)

머브크루 멤버십(월 구독) API입니다. 상품 카탈로그(상품·기수·혜택)는 DB로 관리하며,
스토어 상품 ID는 App Store Connect / Google Play Console 등록값과 동일합니다.

- **인증**: 필요 (JWT Bearer)
- 상품은 **자동갱신 구독**이며, 기수(3기 등)는 가입 시점 라벨입니다(자동갱신되어도 승급 없음).

## 상품 ID 규칙 (참고)

| 플랫폼 | 상품 ID | 관리 |
|---|---|---|
| iOS (App Store) | `com.mub.app.membership.monthly` | DB `membership_plans.ios_product_id` |
| Android (Play) | `mub_membership_monthly` | DB `membership_plans.android_product_id` |

기수마다 상품을 새로 만들지 않습니다. 새 기수는 `membership_cohorts` 에 row 만 추가합니다.

---

## GET `/api/memberships/product` — 멤버십 상품 조회

멤버십 결제 소개 화면에 노출할 **현재 판매 중인 상품**을 조회합니다.
활성 상품(플랜) + 최신 기수 + 현재 가입자 수(ACTIVE)를 반환하며, 앱은 응답의 상품 ID로 스토어 구매를 요청합니다.

- **인증**: 필요 (JWT Bearer)
- **모집 여부(`recruiting`)**: 오늘 날짜가 해당 기수의 모집 기간(시작~종료, 양끝 포함) 안이고 활성 기수이면 `true`

### 요청
```bash
curl "http://localhost:8083/api/memberships/product" \
  -H "Authorization: Bearer <accessToken>"
```

### Success — `200 OK`
```json
{
  "planName": "머브크루",
  "monthlyPrice": 5000,
  "benefits": ["공간 예약 할인", "모임 이용권"],
  "cohortNumber": 3,
  "recruitStartDate": "2026-06-01",
  "recruitEndDate": "2026-06-30",
  "recruiting": true,
  "currentSubscribers": 42,
  "iosProductId": "com.mub.app.membership.monthly",
  "androidProductId": "mub_membership_monthly"
}
```

### Failure
| 상황 | status | 응답 |
|---|---|---|
| 인증 토큰 없음/무효 | 403 | (빈 본문 — 공통 인증 실패 동작) |
| 활성 상품/기수 없음 | 404 | `{"code":"E40404","message":"멤버십 상품을 찾을 수 없습니다", ...}` |

---

## GET `/api/memberships/me` — 내 멤버십 조회

'내 멤버십' 화면용. 현재 **이용 중(ACTIVE)** 인 멤버십을 반환합니다. 없으면 404(클라이언트는 가입 유도 화면 노출).

- **인증**: 필요 (JWT Bearer)

### 요청
```bash
curl "http://localhost:8083/api/memberships/me" -H "Authorization: Bearer <accessToken>"
```

### Success — `200 OK`
```json
{
  "membershipId": 7,
  "memberId": 1,
  "planName": "머브크루",
  "cohortNumber": 3,
  "platform": "APPLE",
  "status": "ACTIVE",
  "startedAt": "2026-08-03T14:00:00",
  "expiresAt": "2026-09-03T14:00:00"
}
```

### Failure
| 상황 | status | 응답 |
|---|---|---|
| 인증 토큰 없음/무효 | 403 | (빈 본문) |
| 이용 중인 멤버십 없음 | 404 | `{"code":"E40405","message":"가입한 멤버십이 없습니다", ...}` |

---

## POST `/api/memberships/purchase` — 멤버십 구매 검증

앱이 스토어 결제를 완료한 뒤 검증 정보를 전달하면, 서버가 **Apple App Store Server API / Google Play Developer API**
로 구매를 검증하고 멤버십을 활성화합니다. 서버는 페이로드가 아닌 **스토어 조회 결과**로 상태를 확정하며,
동일 거래의 재검증은 **멱등**(기존 멤버십 갱신) 처리합니다.

- **인증**: 필요 (JWT Bearer) — 회원은 토큰의 `sub`(memberId)로 식별
- **Body**:
  - `platform` (`APPLE` | `GOOGLE`, 필수)
  - `productId` (필수) — 구매한 스토어 상품 ID
  - `purchaseToken` (필수) — Apple `transactionId` / Google `purchaseToken`
- **검증 규칙**: 스토어 검증 성공 + 상품 ID가 플랜의 해당 플랫폼 상품 ID와 일치해야 활성화. 활성화 시 최신 기수에 귀속.

### 요청
```bash
curl -X POST "http://localhost:8083/api/memberships/purchase" \
  -H "Authorization: Bearer <accessToken>" \
  -H "Content-Type: application/json" \
  -d '{"platform":"APPLE","productId":"com.mub.app.membership.monthly","purchaseToken":"<transactionId>"}'
```

### Success — `200 OK`
```json
{
  "membershipId": 5,
  "memberId": 1,
  "planName": "머브크루",
  "cohortNumber": 3,
  "platform": "APPLE",
  "status": "ACTIVE",
  "startedAt": "2026-08-05T16:00:00",
  "expiresAt": "2026-09-05T16:00:00"
}
```

### Failure
| 상황 | status | 응답 |
|---|---|---|
| 필수 필드 누락 | 400 | `{"code":"E40001", "errors":[...], ...}` |
| 스토어 검증 실패(무효 구매) | 400 | `{"code":"E40013","message":"구매 검증에 실패했습니다", ...}` |
| 구매 상품이 멤버십 상품과 불일치 | 400 | `{"code":"E40014","message":"구매한 상품이 멤버십 상품과 일치하지 않습니다", ...}` |
| 지원하지 않는 플랫폼 | 400 | `{"code":"E40015","message":"지원하지 않는 결제 플랫폼입니다", ...}` |
| 인증 토큰 없음/무효 | 403 | (빈 본문) |

> **스토어 연동 설정**(gitignore 되는 properties): `iap.apple.*`(keyId·issuerId·bundleId·privateKeyPath·baseUrl),
> `iap.google.*`(packageName·serviceAccountKeyPath·baseUrl). `.p8`/서비스계정 JSON 파일은 커밋/이미지에 포함하지 않습니다.
> Google 검증은 서비스계정에 Play Console **재무 데이터 보기** 권한이 부여되어 있어야 합니다.
> ⚠️ 실제 스토어 응답에 대한 **라이브 검증은 샌드박스 계정/실상품 등록 후** 별도 수행이 필요합니다.

---

## POST `/api/memberships/webhook/apple` · `/google` — 스토어 구독 웹훅

Apple(App Store Server Notifications V2)·Google(Play RTDN, Pub/Sub push)이 **구독 상태 변경(갱신·해지·만료)** 을 통지하는 엔드포인트입니다.
서버는 **페이로드를 신뢰하지 않고 거래 식별자만 추출**한 뒤, 스토어에 재조회(구매검증과 동일 경로)하여 상태를 확정하고 멤버십을 갱신합니다.

- **인증**: 불필요 (public) — 스토어 서버가 호출. `/api/memberships/webhook/**` 는 화이트리스트
- **동작**:
  - 유효(갱신·활성) → 멤버십 `ACTIVE` + 만료일 갱신
  - 무효(만료·해지·환불·취소) → 멤버십 `EXPIRED`
  - 알 수 없는 거래/토큰 → 무시(ack)
- **응답**: 항상 `200 OK`(빈 본문)로 ack

### Apple 요청
```bash
curl -X POST "http://localhost:8083/api/memberships/webhook/apple" \
  -H "Content-Type: application/json" \
  -d '{"signedPayload":"<JWS>"}'
```
- `signedPayload`(JWS) → `notificationType` + `data.signedTransactionInfo`(JWS) → `originalTransactionId`·`productId` 추출

### Google 요청
```bash
curl -X POST "http://localhost:8083/api/memberships/webhook/google" \
  -H "Content-Type: application/json" \
  -d '{"message":{"data":"<base64 JSON>","messageId":"..."}}'
```
- `message.data`(base64) → `subscriptionNotification.purchaseToken`·`subscriptionId`·`notificationType` 추출

### Success — `200 OK`
(빈 본문)

> **설정**: Apple은 App Store Connect에 이 URL을 App Store Server Notifications V2로 등록,
> Google은 Play Console RTDN → Pub/Sub 토픽 → 이 URL을 push 구독으로 연결해야 합니다.
> ⚠️ 현재 무효 판정 시 즉시 `EXPIRED` 로 전환합니다(재조회 일시 실패와 실제 만료를 구분하지 않음 — 운영 전 보완 대상).

---

## 초기 데이터(seed) — 참고

`member-service`는 SQL 시더가 없습니다(리소스 seed 미사용). 로컬/운영에서 상품을 노출하려면
아래 seed 를 member-db 초기화(`docker/mysql/member-init`)에 반영하거나 수동 실행하세요.
(스토어 콘솔에도 동일한 상품 ID로 구독 상품을 등록해야 실제 구매가 가능합니다.)

```sql
INSERT INTO membership_plans (name, monthly_price, ios_product_id, android_product_id, active, created_at, updated_at)
VALUES ('머브크루', 5000, 'com.mub.app.membership.monthly', 'mub_membership_monthly', true, NOW(), NOW());

INSERT INTO membership_plan_benefits (plan_id, benefit)
VALUES (1, '공간 예약 할인'), (1, '모임 이용권');

INSERT INTO membership_cohorts (plan_id, cohort_number, recruit_start_date, recruit_end_date, active, created_at, updated_at)
VALUES (1, 3, '2026-06-01', '2026-06-30', true, NOW(), NOW());
```
