# member-service 배포 가이드 (Railway)

`prod` 프로필로 기동한다. 설정 파일(`application-prod.properties`)은 저장소에 커밋되어 있으나
**모든 비밀값은 `${ENV_VAR}` 참조**이므로, 실제 값은 배포 플랫폼 환경변수로 주입해야 한다.

## 기동 방식

Railway가 레포 루트의 `Dockerfile`을 자동 감지한다. 이미지의 기본 프로필은 `docker`이므로
**반드시 `SPRING_PROFILES_ACTIVE=prod`를 설정**해야 한다.

## 필수 환경변수

기본값이 없는 항목이다. 하나라도 빠지면 기동에 실패한다(의도된 fail-fast).

| 변수 | 설명 | 예시 |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | 활성 프로필 | `prod` |
| `DB_URL` | MySQL JDBC URL | `jdbc:mysql://${{MySQL.MYSQLHOST}}:${{MySQL.MYSQLPORT}}/member_service?useSSL=false&characterEncoding=UTF-8&serverTimezone=UTC&allowPublicKeyRetrieval=true` |
| `DB_USERNAME` | DB 계정 | `${{MySQL.MYSQLUSER}}` |
| `DB_PASSWORD` | DB 비밀번호 | `${{MySQL.MYSQLPASSWORD}}` |
| `REDIS_HOST` | Redis 호스트 | `${{Redis.REDISHOST}}` |
| `JWT_SECRET` | JWT 서명 키 (≥32바이트) | `openssl rand -hex 64` 로 발급한 값 |
| `KAKAO_CLIENT_ID` | 카카오 REST API 키 | |
| `KAKAO_REDIRECT_URI` | 카카오 콜백 URL | `https://<배포도메인>/api/auth/kakao/callback` |
| `GROUP_SERVICE_URL` | group-service 주소 | `http://group-service.railway.internal:8081` |
| `APP_PUBLIC_URL` | 이미지 공개 URL 베이스 | `https://<배포도메인>` |
| `CORS_ALLOWED_ORIGINS` | 허용 출처(콤마 구분) | `https://<FE도메인>,capacitor://localhost` |

## 선택 환경변수

| 변수 | 기본값 | 비고 |
|---|---|---|
| `PORT` | `8083` | Railway가 자동 주입 |
| `REDIS_PORT` / `REDIS_PASSWORD` | `6379` / 빈 값 | |
| `DDL_AUTO` | `none` | 스키마는 init SQL로 선주입 |
| `IMAGE_UPLOAD_DIR` | `/app/images` | **영속 볼륨 마운트 경로와 일치시킬 것** |
| `SWAGGER_ENABLED` | `false` | QA 기간에만 `true` |
| `JWT_ACCESS_TOKEN_EXPIRATION` | `86400000` | ms |

## 인앱결제(IAP) 환경변수 — 구독 기능 사용 시에만

스토어 상품 등록 전에는 비워 둬도 기동에 지장이 없다. 멤버십 구매검증(`POST /api/memberships/purchase`)과
구독 웹훅(`POST /api/memberships/webhook/*`) 호출 시점에만 필요하다.

| 변수 | 비고 |
|---|---|
| `IAP_APPLE_KEY_ID` / `IAP_APPLE_ISSUER_ID` / `IAP_APPLE_BUNDLE_ID` | App Store Connect API 키 정보 |
| `IAP_APPLE_PRIVATE_KEY_PATH` | `.p8` 파일 경로 — **저장소·이미지에 포함 금지**, 볼륨/시크릿 파일로 주입 |
| `IAP_GOOGLE_PACKAGE_NAME` | Android 패키지명 |
| `IAP_GOOGLE_SERVICE_ACCOUNT_KEY_PATH` | 서비스계정 JSON 경로 — **저장소·이미지에 포함 금지** |

## 배포 전 체크리스트

- [ ] `SPRING_PROFILES_ACTIVE=prod` 설정 (미설정 시 `docker` 프로필로 떠서 localhost DB를 찾는다)
- [ ] MySQL에 스키마·시드 주입 (`docker/mysql/member-init/01-init.sql`)
      — 지역·관심사 시드가 없으면 온보딩이 실패한다
- [ ] `/app/images`에 영속 볼륨 마운트 — 미마운트 시 재배포마다 프로필 이미지가 전부 소실된다
- [ ] **카카오 개발자센터에 `KAKAO_REDIRECT_URI`와 동일한 값 등록** — 미등록 시 로그인 자체가 불가
- [ ] `JWT_SECRET`은 배포마다 새로 발급 — dev 값 재사용 금지
- [ ] `CORS_ALLOWED_ORIGINS`에 FE 배포 도메인 포함
