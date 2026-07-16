# TRD: 회원 서비스 — 로그인 / 회원가입 기술 설계

> **문서 버전**: 1.0
> **작성일**: 2026-05-31
> **서비스**: member-service
> **베이스 패키지**: `com.yeolcheong.mub.member`

---

## 1. 기술 스택

| 항목 | 스펙 |
|------|------|
| Language | Java 21 |
| Framework | Spring Boot 3.x |
| ORM | Spring Data JPA (Hibernate) |
| DB | MySQL (운영) / H2 (테스트) |
| 토큰 저장소 | Redis (Refresh Token) |
| 외부 API | Kakao OAuth (토큰 발급 / 사용자 정보) |
| HTTP Client | Spring WebClient |
| 인증 | JWT (jjwt) — Access / Refresh |
| 보안 | Spring Security (Stateless) |
| 빌드 | Gradle |

---

## 2. 인증 아키텍처

```
                    ┌──────────────────────────────────────────┐
                    │              member-service              │
  [Client]          │                                          │
   │  ①login(code)  │   AuthController → AuthService           │
   ├───────────────▶│        │                                 │
   │                │        ├─▶ KakaoClient ──②token──▶ Kakao  │
   │                │        │              ◀─③userInfo─        │
   │                │        ├─▶ MemberService (find/create)    │
   │                │        ├─▶ JwtTokenProvider (access/refresh)│
   │                │        └─▶ RefreshTokenRepository (Redis)  │
   │◀──④tokens+isNewMember───┘                                  │
   │                │                                          │
   │  ⑤onboarding   │   OnboardingController → OnboardingService│
   │  (Bearer AT)   │        │ JwtAuthenticationFilter 가 인증   │
   ├───────────────▶│        └─▶ 약관/닉네임/지역/관심사 저장 →ACTIVE│
                    └──────────────────────────────────────────┘
```

- `JwtAuthenticationFilter`가 `Authorization: Bearer {token}`을 파싱하여 `memberId`를 `SecurityContext`에 주입한다.
- `/api/auth/**`, `/api/districts/**`, `/api/interests`, `/api/internal/**`은 인증 없이 접근 허용(permitAll).
- 그 외 경로(`/api/onboarding/**`, `/api/members/**`)는 인증 필요.

---

## 3. 도메인 모델

### 3.1 Member (회원)

```java
// domain/Member.java
@Entity
@Table(name = "members")
@EntityListeners(AuditingEntityListener.class)
public class Member {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "social_provider", nullable = false, length = 20)
    private SnsProvider snsProvider;        // KAKAO

    @Column(nullable = false, unique = true, length = 100)
    private String socialId;                // 카카오 회원번호

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(length = 100)
    private String nickname;                // 온보딩에서 설정

    @Column
    private Long imageId;

    @Transient
    private String imageUrl;

    @Column(length = 50)
    private String regionProvince;          // 시/도 명

    @Column(length = 50)
    private String regionCity;              // 시/군/구 명

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private MemberStatus status = MemberStatus.INACTIVE;

    @CreatedDate    @Column(nullable = false, updatable = false) private LocalDateTime createdAt;
    @LastModifiedDate @Column(nullable = false)                  private LocalDateTime updatedAt;
    @Column private LocalDateTime lastLoginAt;
}
```

**도메인 메서드** (setter 금지 — 의도 드러내는 변경 메서드만):

| 메서드 | 설명 |
|--------|------|
| `updateNickname(String)` | 닉네임 변경 |
| `updateRegion(String province, String city)` | 활동 지역 변경 |
| `updateMemberState(MemberStatus)` | 상태 전환 (INACTIVE → ACTIVE 등) |

---

### 3.2 MemberTermsAgreement (약관 동의)

```java
// domain/MemberTermsAgreement.java
@Entity
@Table(name = "member_terms_agreements")
public class MemberTermsAgreement {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    private Member member;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private TermsType termsType;

    @Column(nullable = false) private Boolean isRequired;  // termsType.isRequired()에서 파생
    @Column(nullable = false) private Boolean agreed;
    @Column private LocalDateTime agreedAt;                // agreed=true 시 now()

    @CreatedDate @Column(nullable = false, updatable = false) private LocalDateTime createdAt;
    @LastModifiedDate @Column(nullable = false)              private LocalDateTime updatedAt;
}
```

- 생성 시 `isRequired`는 `termsType.isRequired()`에서 자동 결정, `agreedAt`은 동의 여부에 따라 설정.
- `updateAgreed(Boolean)`로 재동의/철회 가능.

---

### 3.3 MemberInterest / MemberInterestOption (관심사 · 옵션)

```java
// domain/MemberInterest.java
@Entity
@Table(name = "member_interests",
       uniqueConstraints = @UniqueConstraint(columnNames = {"member_id", "interest_type"}))
public class MemberInterest {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    private Member member;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private InterestType interestType;

    @OneToMany(mappedBy = "memberInterest", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MemberInterestOption> options = new ArrayList<>();
}
```

- `addOption(InterestOption)` — 중복 옵션은 무시하고 추가.
- `replaceOptions(List<InterestOption>)` — 전체 교체.
- 회원-관심사 조합은 유니크 (`member_id` + `interest_type`).

```java
// domain/MemberInterestOption.java
@Entity
@Table(name = "member_interest_options",
       uniqueConstraints = @UniqueConstraint(columnNames = {"member_interest_id", "option_type"}))
public class MemberInterestOption {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_interest_id", nullable = false)
    private MemberInterest memberInterest;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private InterestOption optionType;
}
```

---

### 3.4 District (지역)

```java
// domain/District.java
@Entity
@Table(name = "districts",
       uniqueConstraints = @UniqueConstraint(columnNames = {"dist_code1", "dist_code2"}))
public class District {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 10)  private String distCode1;       // 시/도 코드
    @Column(name = "dist_code1_name", nullable = false, length = 50) private String distCode1Name;
    @Column(nullable = false, length = 10)  private String distCode2;       // 시/군/구 코드
    @Column(name = "dist_code2_name", nullable = false, length = 50) private String distCode2Name;
}
```

- 지역 데이터는 사전 적재(seed). 온보딩 시 `distCode1` + `distCode2` 조합 존재 여부로 유효성 검증.

---

### 3.5 RefreshToken (Redis)

```java
// domain/RefreshToken.java  — JPA Entity 아님, Redis 저장용 값 객체
public class RefreshToken {
    private Long memberId;
    private String token;
    private LocalDateTime expiresAt;

    public boolean isExpired() { return LocalDateTime.now().isAfter(expiresAt); }
}
```

- `RefreshTokenRepository.save(token, ttlSeconds)`로 TTL과 함께 저장.
- `findByMemberId(Long)`로 재발급 시 저장된 토큰과 대조.

---

### 3.6 Enum 정의

```java
// domain/SnsProvider.java
public enum SnsProvider { KAKAO }

// domain/MemberStatus.java
public enum MemberStatus {
    MUBACTIVE,   // 머브 구독
    ACTIVE,      // 회원가입 완료
    INACTIVE,    // 가입 진행 중 / 휴면
    DELETED      // 탈퇴
}

// domain/TermsType.java  (description, required)
public enum TermsType {
    TERMS_OF_SERVICE("서비스 이용약관 동의", true),
    PRIVACY_POLICY("개인정보 처리방침", true),
    LOCATION_SERVICE("위치서비스 이용약관 동의", true),
    MARKETING("마케팅 수신 동의", false);
}

// domain/InterestType.java  — 관심사별 선택 가능한 InterestOption 목록을 보유
public enum InterestType {
    SELF_DEVELOPMENT("자기계발", [...]),   INVESTMENT("재테크/투자", [...]),
    READING("독서/글쓰기", [...]),         EDUCATION("교육/멘토링", [...]),
    SPORTS("운동/스포츠", [...]),          RESTAURANT("맛집/카페", [...]),
    MUSIC("음악/악기", [...]),             DANCE("댄스/무용/연기", [...]),
    PHOTO("사진/영상", [...]);
}

// domain/InterestOption.java  — 공간 옵션 (description)
//   공통(자기계발/재테크/독서/교육): INTERNET_AVAILABLE, CONTENT_AVAILABLE,
//     PARTITION_SPACE, WINDOW_SEAT, SINGLE_TABLE, MEETING_ROOM, WIDE_TABLE,
//     COMFORTABLE_CHAIR, QUIET_MUSIC, WHITE_NOISE, CLEAN_RESTROOM,
//     SEPARATE_RESTROOM, NO_KIDS_ZONE, NO_SENIOR_ZONE, SMOKING_AREA,
//     BEAM_PROJECTOR, MEETING_SPACE, OUTSIDE_FOOD_OK, PARKING, TEAM_PROJECT, SNACK_PROVIDED
//   운동/스포츠: GRASS_FIELD, PARKING, SHOWER, EQUIPMENT_RENTAL, LESSON,
//     INDOOR, OUTDOOR, LOCKER, SEPARATE_RESTROOM
//   맛집/카페: PET_FRIENDLY, PARKING, OCEAN_VIEW, RIVER_VIEW, MOUNTAIN_VIEW, CITY_VIEW
//   음악/악기: SOUNDPROOF, RECORDING, EQUIPMENT_RENTAL, SEPARATE_RESTROOM
//   댄스/무용/연기: FULL_LENGTH_MIRROR, EQUIPMENT_RENTAL, BEAM_PROJECTOR, LOCKER, SEPARATE_RESTROOM, SHOWER
//   사진/영상: RENTAL_STUDIO, EQUIPMENT_RENTAL, SEPARATE_RESTROOM
```

- 관심사별 허용 옵션은 `InterestType.availableOptions`로 관리되며, 온보딩 시 서버가 이 집합으로 옵션 유효성을 검증한다.

---

## 4. API 엔드포인트

> 인증이 필요한 엔드포인트는 `Authorization: Bearer {accessToken}` 헤더 필수.

### 4.1 인증 (AuthController — `/api/auth`)

| Method | Path | Description | 인증 |
|--------|------|-------------|------|
| `GET`  | `/api/auth/login/url` | 카카오 인증 URL 조회 | 불필요 |
| `GET`  | `/api/auth/kakao/callback?code=` | 카카오 redirect 콜백 처리 | 불필요 |
| `POST` | `/api/auth/login` | 인가 코드로 로그인 | 불필요 |
| `POST` | `/api/auth/login/token` | 카카오 SDK access token으로 로그인 | 불필요 |
| `POST` | `/api/auth/refresh` | Refresh Token으로 Access Token 재발급 | 불필요 |

### 4.2 온보딩 (OnboardingController — `/api/onboarding`)

| Method | Path | Description | 인증 |
|--------|------|-------------|------|
| `POST` | `/api/onboarding/complete` | 약관·닉네임·지역·관심사 일괄 저장, 회원 활성화 | **필요** |

### 4.3 지역 (DistrictController — `/api/districts`)

| Method | Path | Description | 인증 |
|--------|------|-------------|------|
| `GET` | `/api/districts/code1` | 시/도 목록 | 불필요 |
| `GET` | `/api/districts/code2?distCode1=` | 특정 시/도의 시/군/구 목록 | 불필요 |

### 4.4 관심사 (InterestController — `/api/interests`)

| Method | Path | Description | 인증 |
|--------|------|-------------|------|
| `GET` | `/api/interests` | 관심사 + 관심사별 옵션 전체 목록 | 불필요 |

### 4.5 회원 조회 (MemberController / InternalMemberController)

| Method | Path | Description | 인증 |
|--------|------|-------------|------|
| `GET` | `/api/members?email=` | 이메일로 회원 조회 | 필요 |
| `GET` | `/api/internal/members?ids=` | ID 목록으로 회원 요약 일괄 조회 (서비스 간) | 내부 전용 |

> `/api/internal/**`는 게이트웨이가 외부에 노출하지 않아야 하는 서비스 간 호출용 경로.

---

## 5. DTO 설계

### 5.1 로그인 Request

```java
// dto/LoginRequest.java — 인가 코드 방식
public class LoginRequest {
    @NotBlank private String code;
}

// dto/KakaoTokenLoginRequest.java — SDK 토큰 방식
public class KakaoTokenLoginRequest {
    @NotBlank private String accessToken;
}

// dto/TokenRefreshRequest.java
public class TokenRefreshRequest {
    @NotBlank private String refreshToken;
}
```

### 5.2 로그인 Response

```java
// dto/LoginResponse.java
@Getter @Builder @AllArgsConstructor
public class LoginResponse {
    // 토큰 정보
    private String tokenType;            // "Bearer"
    private String accessToken;
    private Long   expiresIn;            // access token 만료(초)
    private String refreshToken;
    private Long   refreshTokenExpiresIn;
    // 카카오 계정 정보
    private String kakaoId;
    private String connectedAt;
    private String email;
    // 회원 메타
    private Boolean isNewMember;         // true → 온보딩 진입
    private String  memberId;

    public static LoginResponse of(String accessToken, String refreshToken, Long expiresIn,
                                   SnsUserInfoResponse snsUserInfo, Boolean isNewMember, String memberId) { ... }
}

// dto/TokenRefreshResponse.java — 재발급 응답 (isNewMember=false 고정)
```

### 5.3 카카오 사용자 정보 (외부 API 매핑)

```java
// dto/SnsUserInfoResponse.java  (카카오 /v2/user/me 응답)
public class SnsUserInfoResponse {
    @JsonProperty("id")           private Long id;          // 카카오 회원번호
    @JsonProperty("connected_at") private String connectedAt;
    @JsonProperty("kakao_account") private KakaoAccount kakaoAccount;  // email 포함

    public String getKakaoIdAsString();  // String.valueOf(id)
    public String getEmail();            // kakaoAccount.email
}
// dto/LoginTokenResponse.java — 카카오 토큰 응답(access_token 등) 매핑
```

### 5.4 온보딩 Request

```java
// dto/OnboardingRequest.java
@Getter @Builder @NoArgsConstructor @AllArgsConstructor
public class OnboardingRequest {

    @NotNull @Valid
    private TermsAgreementRequest termsAgreementRequest;

    @NotBlank(message = "닉네임은 필수입니다")
    @Size(max = 10, message = "닉네임은 최대 10자까지 가능합니다")
    @Pattern(regexp = "^[가-힣a-zA-Z0-9]+$", message = "닉네임은 한글, 영문, 숫자만 가능합니다")
    private String nickname;

    @NotBlank(message = "활동 지역(시/도)은 필수입니다")
    private String distCode1;

    @NotBlank(message = "활동 지역(구/시)은 필수입니다")
    private String distCode2;

    @NotNull
    @Size(min = 1, max = 3, message = "관심사는 1개 이상 3개 이하로 선택해야 합니다")
    @Valid
    private List<InterestRequest> interests;

    // 약관 동의 — 필수 3종은 @AssertTrue
    public static class TermsAgreementRequest {
        @NotNull @AssertTrue(message = "서비스 이용약관에 동의해야 합니다")    private Boolean termsOfService;
        @NotNull @AssertTrue(message = "개인정보 처리방침에 동의해야 합니다")  private Boolean privacyPolicy;
        @NotNull @AssertTrue(message = "위치기반 서비스 이용약관에 동의해야 합니다") private Boolean locationService;
        private Boolean marketing;   // 선택
        public Map<TermsType, Boolean> toMap() { ... }
    }

    // 관심사별 옵션
    public static class InterestRequest {
        @NotNull private InterestType interestType;
        // 선호 편의시설(옵션)은 선택 사항 — 미입력/0개 허용
        private List<InterestOption> options;
    }
}
```

### 5.5 온보딩 Response

```java
// dto/OnboardingResponse.java
public class OnboardingResponse {
    private Long   memberId;
    private String nickname;
    private String message;   // "회원가입이 완료되었습니다"
    public static OnboardingResponse of(Long memberId, String nickname) { ... }
}
```

### 5.6 조회 Response

```java
// dto/DistrictResponse.java
//   DistCode1 { distCode1, distCode1Name }
//   DistCode2 { distCode2, distCode2Name }

// dto/InterestResponse.java
//   { interestType, description, options: [{ optionType, description }] }

// dto/MemberInfoResponse.from(Member)        — 단건 회원 정보
// dto/MemberSummaryResponse.from(Member)     — 서비스 간 요약(id, nickname, email 등)
```

---

## 6. 서비스 레이어 설계

### 6.1 AuthService

```
AuthService                       (@Transactional(readOnly = true) 기본)
├── getAuthUrl()
│     KakaoClient.buildAuthorizationUrl()
│
├── @Transactional login(String code)
│     ① KakaoClient.fetchAccessToken(code)
│     ② KakaoClient.fetchUserInfo(accessToken)
│     ③ issueTokensForKakaoUser(...)
│
├── @Transactional loginWithKakaoToken(String kakaoAccessToken)
│     ① KakaoClient.fetchUserInfo(kakaoAccessToken)
│     ② issueTokensForKakaoUser(...)
│
├── @Transactional refreshAccessToken(String refreshToken)
│     ① JWT 유효성 검증 → INVALID_TOKEN
│     ② Redis 저장 토큰과 대조 → 불일치 시 INVALID_TOKEN
│     ③ 새 Access/Refresh 발급 + Redis 갱신
│
└── (private) issueTokensForKakaoUser(SnsUserInfoResponse)
      - findBySocialId 로 신규/기존 분기
      - 신규 → MemberService.createdFromSnsUser (INACTIVE)
      - Access/Refresh 발급 후 saveRefreshToken
      - LoginResponse.of(...) 반환
```

### 6.2 OnboardingService

```
OnboardingService                 (@Transactional(readOnly = true) 기본)
└── @Transactional completeOnboarding(Long memberId, OnboardingRequest)
      1. memberRepository.findById → 없으면 MEMBER_NOT_FOUND
      2. status ∈ {ACTIVE, MUBACTIVE} → ALREADY_ONBOARDED
      3. saveTermsAgreements(...)   — 필수 약관 미동의 시 TERMS_AGREEMENT_REQUIRED
      4. updateNickname(...)
      5. updateRegion(...)          — distCode 조합 검증, INVALID_DISTRICT_CODE
      6. saveInterests(...)         — 1~3개 / 옵션 1개 이상 / 허용 옵션 검증
      7. member.updateMemberState(ACTIVE) + save
      → OnboardingResponse.of(...)
```

검증 규칙 상세:
- `saveTermsAgreements`: `TermsType.values()`를 순회하며 필수 약관 동의 여부 확인.
- `saveInterests`: `interestRequests` 크기 1~3 강제, 기존 관심사 삭제 후 재저장.
- `validateInterestOptions`: 옵션 비어있으면 `INTEREST_OPTION_REQUIRED`, 허용 집합 밖이면 `INVALID_INTEREST_OPTION`.

### 6.3 MemberService

```
MemberService                     (@Transactional(readOnly = true) 기본)
├── findById(Long)                              → MEMBER_NOT_FOUND
├── findBySocialId(SnsProvider, String)         → Optional<Member>
├── @Transactional createdFromSnsUser(SnsUserInfoResponse)  → INACTIVE 회원 생성
├── getMemberByEmail(String)                    → MemberInfoResponse
└── findSummariesByIds(Collection<Long>)        → 존재하는 회원만 요약 반환
```

### 6.4 DistrictService / InterestService

```
DistrictService
├── getDistCode1List()                  → 시/도 목록 (중복 제거)
└── getDistCode2List(String distCode1)  → 시/군/구 목록

InterestService
└── getAllInterests()                   → InterestType + availableOptions 전체
```

---

## 7. Repository 설계

```java
// repository/MemberRepository.java
@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findBySnsProviderAndSocialId(SnsProvider snsProvider, String socialId);
    Optional<Member> findByEmail(String email);
}

// repository/MemberTermsAgreementRepository.java
@Repository
public interface MemberTermsAgreementRepository extends JpaRepository<MemberTermsAgreement, Long> {
    // saveAll 로 약관 일괄 저장
}

// repository/MemberInterestRepository.java
@Repository
public interface MemberInterestRepository extends JpaRepository<MemberInterest, Long> {
    void deleteByMemberId(Long memberId);   // 관심사 재저장 전 초기화
}

// repository/DistrictRepository.java
@Repository
public interface DistrictRepository extends JpaRepository<District, Long> {
    Optional<District> findByDistCode1AndDistCode2(String distCode1, String distCode2);
    // 시/도, 시/군/구 목록 조회
}

// repository/RefreshTokenRepository.java — Redis 기반
//   save(RefreshToken, ttlSeconds), findByMemberId(Long) : Optional<String>
```

---

## 8. 보안 설정 (SecurityConfig)

```
- CSRF: disable
- Session: STATELESS
- permitAll: /api/auth/**, /api/districts/**, /api/interests, /api/internal/**
             + 정적 리소스, /actuator/**
- anyRequest: authenticated
- JwtAuthenticationFilter 를 UsernamePasswordAuthenticationFilter 앞에 추가
- CORS: http://localhost:3000, capacitor://localhost, ionic://localhost
        Methods: GET/POST/PUT/PATCH/DELETE/OPTIONS
        ExposedHeaders: Authorization, Refresh-Token
```

### JwtTokenProvider
- `generateAccessToken(memberId, socialId, snsProvider)` — claims: socialId, snsProvider, type=ACCESS, subject=memberId.
- `generateRefreshToken(memberId)` — type=REFRESH.
- HMAC-SHA 서명 (`JwtProperties.secret`), 만료는 `accessTokenExpiration` / `refreshTokenExpiration`.
- `validateToken`, `getMemberIdFromToken` 제공.

### JwtAuthenticationFilter
- `Authorization: Bearer ...`에서 토큰 추출 → 검증 성공 시 `memberId`를 principal로 `SecurityContext`에 주입.
- `/actuator` 경로는 필터 스킵.

---

## 9. 외부 연동 — KakaoClient

```
KakaoClient (WebClient 기반)
├── buildAuthorizationUrl()
│     authUrl?client_id=&redirect_uri=&response_type=code
├── fetchAccessToken(code)  → POST tokenUrl (form-urlencoded)
│     실패 매핑: 400 → INVALID_TOKEN_VALUE, 401 → INVALID_TOKEN, 그 외 → EXTERNAL_API_ERROR
└── fetchUserInfo(accessToken) → GET userInfoUrl (Bearer)
      401 → INVALID_TOKEN, 그 외 → EXTERNAL_API_ERROR
```

- 설정값은 `KakaoProperties` (`authUrl`, `tokenUrl`, `userInfoUrl`, `clientId`, `redirectUri`)로 외부화.

---

## 10. 예외 코드 (ErrorCode)

| 코드 | HTTP | 메시지 |
|------|------|--------|
| `INVALID_INPUT_VALUE` (E40001) | 400 | 입력값이 올바르지 않습니다 |
| `INVALID_TYPE_VALUE` (E40002) | 400 | 타입이 올바르지 않습니다 |
| `MISSING_INPUT_VALUE` (E40003) | 400 | 필수 입력값이 누락되었습니다 |
| `INVALID_TOKEN_VALUE` (E40004) | 400 | 유효하지 않은 인가 코드입니다 |
| `ALREADY_ONBOARDED` (E40005) | 400 | 이미 회원가입이 완료된 회원입니다 |
| `TERMS_AGREEMENT_REQUIRED` (E40006) | 400 | 필수 약관에 동의해야 합니다 |
| `INVALID_INTEREST_COUNT` (E40007) | 400 | 관심사는 1개 이상 3개 이하로 선택해야 합니다 |
| `INTEREST_OPTION_REQUIRED` (E40008) | — | (미사용) 옵션이 선택 사항으로 변경되어 더 이상 발생하지 않음 |
| `INVALID_INTEREST_OPTION` (E40009) | 400 | 해당 관심사에 유효하지 않은 옵션입니다 |
| `INVALID_DISTRICT_CODE` (E40010) | 400 | 유효하지 않은 지역 코드입니다 |
| `MEMBER_NOT_FOUND` (E40401) | 404 | 회원을 찾을 수 없습니다 |
| `DUPLICATE_NICKNAME` (E40901) | 409 | 이미 사용 중인 닉네임입니다 |
| `DUPLICATE_EMAIL` (E40902) | 409 | 이미 사용 중인 이메일입니다 |
| `INVALID_TOKEN` (E40101) | 401 | 유효하지 않은 토큰입니다 |
| `EXPIRED_TOKEN` (E40102) | 401 | 만료된 토큰입니다 |
| `ACCESS_DENIED` (E40301) | 403 | 접근 권한이 없습니다 |
| `INTERNAL_SERVER_ERROR` (E50001) | 500 | 서버 내부 오류가 발생했습니다 |
| `EXTERNAL_API_ERROR` (E50002) | 500 | 외부 API 호출 중 오류가 발생했습니다 |

- 전역 처리: `@RestControllerAdvice` (`GlobalExceptionHandler`) → `ErrorResponse(code, message, status)` 형식 반환.

---

## 11. 디자인-구현 정합성 갭 (보강 권고)

| 항목 | 디자인 요구 | 현재 구현 | 권고 |
|------|------------|-----------|------|
| 닉네임 최소 길이 | 2자 이상 | `@Size(max=10)`만, 1자 통과 | `@Size(min=2, max=10)`로 변경 |
| 관심사별 옵션 상한 | 관심사별 최대 5개 | 상한 미검증 | `validateInterestOptions`에 5개 상한 추가 검토 |
| 닉네임 중복 | 디자인 무관(서버 정책) | 코드만 정의, 검증 미적용 | 중복 검사 API 또는 온보딩 내 검증 추가 검토 |
| 가입 축하 쿠폰 | 3회 이용권 발급 | 미구현 | 별도 혜택/쿠폰 도메인 설계 |

> 본 문서는 **현재 구현 상태를 기준**으로 작성되었으며, 위 갭은 후속 `feat`/`fix` 작업으로 분리하여 반영한다.

---

## 12. 데이터베이스 스키마 (DDL 요약)

```sql
CREATE TABLE members (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    social_provider VARCHAR(20)  NOT NULL,
    social_id       VARCHAR(100) NOT NULL UNIQUE,
    email           VARCHAR(100) NOT NULL UNIQUE,
    nickname        VARCHAR(100),
    image_id        BIGINT,
    region_province VARCHAR(50),
    region_city     VARCHAR(50),
    status          VARCHAR(20)  NOT NULL DEFAULT 'INACTIVE',
    created_at      DATETIME     NOT NULL,
    updated_at      DATETIME     NOT NULL,
    last_login_at   DATETIME
);

CREATE TABLE member_terms_agreements (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id   BIGINT      NOT NULL,
    terms_type  VARCHAR(50) NOT NULL,
    is_required BOOLEAN     NOT NULL,
    agreed      BOOLEAN     NOT NULL,
    agreed_at   DATETIME,
    created_at  DATETIME    NOT NULL,
    updated_at  DATETIME    NOT NULL,
    CONSTRAINT fk_terms_member FOREIGN KEY (member_id) REFERENCES members(id)
);

CREATE TABLE member_interests (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id     BIGINT      NOT NULL,
    interest_type VARCHAR(50) NOT NULL,
    created_at    DATETIME    NOT NULL,
    UNIQUE KEY uk_member_interest (member_id, interest_type),
    CONSTRAINT fk_interest_member FOREIGN KEY (member_id) REFERENCES members(id)
);

CREATE TABLE member_interest_options (
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_interest_id BIGINT      NOT NULL,
    option_type        VARCHAR(50) NOT NULL,
    created_at         DATETIME    NOT NULL,
    UNIQUE KEY uk_interest_option (member_interest_id, option_type),
    CONSTRAINT fk_option_interest FOREIGN KEY (member_interest_id) REFERENCES member_interests(id)
);

CREATE TABLE districts (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    dist_code1      VARCHAR(10) NOT NULL,
    dist_code1_name VARCHAR(50) NOT NULL,
    dist_code2      VARCHAR(10) NOT NULL,
    dist_code2_name VARCHAR(50) NOT NULL,
    created_at      DATETIME    NOT NULL,
    UNIQUE KEY uk_district (dist_code1, dist_code2)
);

-- refresh_token: Redis (key=memberId, value=token, TTL=refreshTokenExpiresIn)
```

---

## 13. 패키지 구조 (관련 파일)

```
com.yeolcheong.mub.member
├── client/
│   └── KakaoClient.java
├── config/
│   ├── KakaoProperties.java   RedisConfig.java   SecurityConfig.java
│   ├── WebClientConfig.java   JpaConfig.java
├── controller/
│   ├── AuthController.java         OnboardingController.java
│   ├── DistrictController.java     InterestController.java
│   ├── MemberController.java       InternalMemberController.java
├── domain/
│   ├── Member.java                 MemberStatus.java     SnsProvider.java
│   ├── MemberTermsAgreement.java   TermsType.java
│   ├── MemberInterest.java         MemberInterestOption.java
│   ├── InterestType.java           InterestOption.java
│   ├── District.java               RefreshToken.java
├── dto/
│   ├── LoginRequest.java           KakaoTokenLoginRequest.java
│   ├── LoginResponse.java          LoginTokenResponse.java
│   ├── TokenRefreshRequest.java    TokenRefreshResponse.java
│   ├── SnsUserInfoResponse.java    OnboardingRequest.java
│   ├── OnboardingResponse.java     DistrictResponse.java
│   ├── InterestResponse.java       MemberInfoResponse.java
│   └── MemberSummaryResponse.java
├── exception/
│   ├── ErrorCode.java              ErrorResponse.java
│   ├── CommonException.java        MemberServiceApiException.java
│   ├── OnboardingServiceApiException.java   GlobalExceptionHandler.java
├── repository/
│   ├── MemberRepository.java       MemberTermsAgreementRepository.java
│   ├── MemberInterestRepository.java   DistrictRepository.java
│   └── RefreshTokenRepository.java
├── security/
│   ├── JwtAuthenticationFilter.java   JwtTokenProvider.java   JwtProperties.java
└── service/
    ├── AuthService.java            OnboardingService.java
    ├── MemberService.java          DistrictService.java   InterestService.java
```

---

## 14. 구현 우선순위

| 우선순위 | 기능 | 관련 클래스 |
|----------|------|-------------|
| P0 | 카카오 로그인 / 토큰 발급 | AuthService, KakaoClient, JwtTokenProvider |
| P0 | 토큰 재발급 | AuthService.refreshAccessToken, RefreshTokenRepository |
| P0 | 온보딩 완료 | OnboardingService, OnboardingController |
| P1 | 지역 / 관심사 목록 조회 | DistrictService, InterestService |
| P1 | 닉네임 min(2) / 옵션 상한(5) 보강 | OnboardingRequest, OnboardingService (§11) |
| P2 | 닉네임 중복 검사 API | MemberService (신규) |
| P2 | 가입 축하 쿠폰 발급 | 별도 혜택/쿠폰 도메인 |
