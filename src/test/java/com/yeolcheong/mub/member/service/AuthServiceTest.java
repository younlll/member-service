package com.yeolcheong.mub.member.service;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.SoftAssertions.*;
import static org.mockito.BDDMockito.*;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.yeolcheong.mub.member.config.KakaoProperties;
import com.yeolcheong.mub.member.domain.MemberStatus;
import com.yeolcheong.mub.member.domain.SnsProvider;
import com.yeolcheong.mub.member.domain.Member;
import com.yeolcheong.mub.member.dto.LoginResponse;
import com.yeolcheong.mub.member.dto.LoginTokenResponse;
import com.yeolcheong.mub.member.dto.SnsUserInfoResponse;
import com.yeolcheong.mub.member.dto.TokenRefreshResponse;
import com.yeolcheong.mub.member.exception.ErrorCode;
import com.yeolcheong.mub.member.exception.MemberServiceApiException;
import com.yeolcheong.mub.member.repository.RefreshTokenRepository;
import com.yeolcheong.mub.member.security.JwtTokenProvider;

import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AuthService")
class AuthServiceTest {

	private static final Long TEST_MEMBER_ID = 1L;
	private static final String TEST_SOCIAL_ID = "1212343456";
	private static final String TEST_EMAIL = "kakaoLoginTest@example.com";
	private static final String TEST_ACCESS_TOKEN = "jwt-access-token";
	private static final String TEST_REFRESH_TOKEN = "jwt-refresh-token";
	@Mock
	private KakaoProperties kakaoProperties;
	@Mock
	private WebClient webClient;
	@Mock
	private WebClient.RequestBodyUriSpec requestBodyUriSpec;
	@Mock
	private WebClient.RequestBodySpec requestBodySpec;
	@Mock
	private WebClient.RequestHeadersSpec<?> requestHeadersSpec;
	@Mock
	private WebClient.RequestHeadersUriSpec<?> requestHeadersUriSpec;
	@Mock
	private WebClient.ResponseSpec responseSpec;
	@Mock
	private MemberService memberService;
	@Mock
	private JwtTokenProvider jwtTokenProvider;
	@Mock
	private RefreshTokenRepository refreshTokenRepository;
	@InjectMocks
	private AuthService authService;
	private Member activeMember;
	private Member inactiveMember;

	@BeforeEach
	void setUp() {
		given(kakaoProperties.getAuthUrl()).willReturn("https://kauth.kakao.com/oauth/authorize");
		given(kakaoProperties.getClientId()).willReturn("test-client-id");
		given(kakaoProperties.getRedirectUri()).willReturn("http://localhost:8083/api/auth/kakao/callback");
		given(kakaoProperties.getTokenUrl()).willReturn("https://kauth.kakao.com/oauth/token");
		given(kakaoProperties.getUserInfoUrl()).willReturn("https://kapi.kakao.com/v2/user/me");

		activeMember = Member.builder()
			.id(TEST_MEMBER_ID)
			.snsProvider(SnsProvider.KAKAO)
			.socialId(TEST_SOCIAL_ID)
			.email(TEST_EMAIL)
			.status(MemberStatus.ACTIVE)
			.lastLoginAt(LocalDateTime.now())
			.build();

		inactiveMember = Member.builder()
			.id(2L)
			.snsProvider(SnsProvider.KAKAO)
			.socialId(TEST_SOCIAL_ID)
			.email(null)
			.status(MemberStatus.INACTIVE)
			.lastLoginAt(LocalDateTime.now())
			.build();
	}

	@SuppressWarnings("unchecked")
	private void setupTokenWebClientMock(Mono<LoginTokenResponse> mono) {
		given(webClient.post()).willReturn(requestBodyUriSpec);
		given(requestBodyUriSpec.uri(anyString())).willReturn(requestBodySpec);
		given(requestBodySpec.contentType(any())).willReturn(requestBodySpec);
		given(requestBodySpec.bodyValue(any())).willAnswer(inv -> requestHeadersSpec);
		given(requestHeadersSpec.retrieve()).willReturn(responseSpec);
		given(responseSpec.bodyToMono(LoginTokenResponse.class)).willReturn(mono);
	}

	@SuppressWarnings("unchecked")
	private void setupUserInfoWebClientMock(Mono<SnsUserInfoResponse> mono) {
		given(webClient.get()).willAnswer(inv -> requestHeadersUriSpec);
		given(requestHeadersUriSpec.uri(anyString())).willAnswer(inv -> requestHeadersUriSpec);
		given(requestHeadersUriSpec.header(anyString(), anyString())).willAnswer(inv -> requestHeadersUriSpec);
		given(requestHeadersUriSpec.retrieve()).willReturn(responseSpec);
		given(responseSpec.bodyToMono(SnsUserInfoResponse.class)).willReturn(mono);
	}

	private WebClientResponseException makeWebClientException(HttpStatus status) {
		return WebClientResponseException.create(
			status.value(), status.getReasonPhrase(), null,
			("{\"error\":\"" + status.name() + "\"}").getBytes(), null
		);
	}

	// =========================================================
	// Helper
	// =========================================================

	private LoginTokenResponse createTokenResponse() {
		return LoginTokenResponse.builder()
			.tokenType("Bearer")
			.accessToken("kakao-access-token")
			.expiresIn(21599)
			.refreshToken("kakao-refresh-token")
			.refreshTokenExpiresIn(5183999)
			.build();
	}

	private SnsUserInfoResponse createKakaoUserInfo(String socialId, String email) {
		return SnsUserInfoResponse.builder()
			.id(Long.parseLong(socialId))
			.connectedAt("2025-10-11T09:00:00Z")
			.kakaoAccount(SnsUserInfoResponse.KakaoAccount.builder().email(email).build())
			.build();
	}

	// =========================================================
	// getAuthUrl
	// =========================================================
	@Nested
	@DisplayName("getAuthUrl")
	class GetAuthUrl {

		@Test
		@DisplayName("should return url containing all required oauth parameters")
		void shouldReturnUrlContainingAllRequiredOauthParameters() {
			// when
			String authUrl = authService.getAuthUrl();

			// then
			assertSoftly(softly -> {
				softly.assertThat(authUrl).contains("https://kauth.kakao.com/oauth/authorize");
				softly.assertThat(authUrl).contains("client_id=test-client-id");
				softly.assertThat(authUrl).contains("redirect_uri=http://localhost:8083/api/auth/kakao/callback");
				softly.assertThat(authUrl).contains("response_type=code");
			});
		}

		@Test
		@DisplayName("should return url starting with kakao auth base url")
		void shouldReturnUrlStartingWithKakaoAuthBaseUrl() {
			// when
			String authUrl = authService.getAuthUrl();

			// then
			assertThat(authUrl).startsWith("https://kauth.kakao.com/oauth/authorize");
		}

		@Test
		@DisplayName("should reflect dynamic clientId in auth url")
		void shouldReflectDynamicClientIdInAuthUrl() {
			// given
			given(kakaoProperties.getClientId()).willReturn("another-client-id");

			// when
			String authUrl = authService.getAuthUrl();

			// then
			assertThat(authUrl).contains("client_id=another-client-id");
		}
	}

	// =========================================================
	// login
	// =========================================================
	@Nested
	@DisplayName("login")
	class Login {

		@Test
		@DisplayName("should create new member and return isNewMember=true on first login")
		void shouldCreateNewMemberAndReturnIsNewMemberTrueOnFirstLogin() {
			// given
			setupTokenWebClientMock(Mono.just(createTokenResponse()));
			setupUserInfoWebClientMock(Mono.just(createKakaoUserInfo(TEST_SOCIAL_ID, TEST_EMAIL)));

			given(memberService.findBySocialId(SnsProvider.KAKAO, TEST_SOCIAL_ID)).willReturn(Optional.empty());
			given(memberService.createdFromSnsUser(any())).willReturn(inactiveMember);
			given(jwtTokenProvider.generateAccessToken(anyLong(), anyString(), anyString())).willReturn(
				TEST_ACCESS_TOKEN);
			given(jwtTokenProvider.generateRefreshToken(anyLong())).willReturn(TEST_REFRESH_TOKEN);
			given(jwtTokenProvider.getAccessTokenExpiresIn()).willReturn(86400L);

			// when
			LoginResponse response = authService.login("auth-code");

			// then
			assertSoftly(softly -> {
				softly.assertThat(response.getIsNewMember()).isTrue();
				softly.assertThat(response.getAccessToken()).isEqualTo(TEST_ACCESS_TOKEN);
				softly.assertThat(response.getRefreshToken()).isEqualTo(TEST_REFRESH_TOKEN);
				softly.assertThat(response.getTokenType()).isEqualTo("Bearer");
				softly.assertThat(response.getExpiresIn()).isEqualTo(86400L);
			});
			verify(memberService, times(1)).createdFromSnsUser(any());
		}

		@Test
		@DisplayName("should skip member creation and return isNewMember=false on existing member login")
		void shouldSkipMemberCreationAndReturnIsNewMemberFalseOnExistingMemberLogin() {
			// given
			setupTokenWebClientMock(Mono.just(createTokenResponse()));
			setupUserInfoWebClientMock(Mono.just(createKakaoUserInfo(TEST_SOCIAL_ID, TEST_EMAIL)));

			given(memberService.findBySocialId(SnsProvider.KAKAO, TEST_SOCIAL_ID)).willReturn(
				Optional.of(activeMember));
			given(jwtTokenProvider.generateAccessToken(anyLong(), anyString(), anyString())).willReturn(
				TEST_ACCESS_TOKEN);
			given(jwtTokenProvider.generateRefreshToken(anyLong())).willReturn(TEST_REFRESH_TOKEN);
			given(jwtTokenProvider.getAccessTokenExpiresIn()).willReturn(86400L);

			// when
			LoginResponse response = authService.login("auth-code");

			// then
			assertThat(response.getIsNewMember()).isFalse();
			verify(memberService, never()).createdFromSnsUser(any());
		}

		@Test
		@DisplayName("should save refresh token to redis on login")
		void shouldSaveRefreshTokenToRedisOnLogin() {
			// given
			setupTokenWebClientMock(Mono.just(createTokenResponse()));
			setupUserInfoWebClientMock(Mono.just(createKakaoUserInfo(TEST_SOCIAL_ID, TEST_EMAIL)));

			given(memberService.findBySocialId(SnsProvider.KAKAO, TEST_SOCIAL_ID)).willReturn(
				Optional.of(activeMember));
			given(jwtTokenProvider.generateAccessToken(anyLong(), anyString(), anyString())).willReturn(
				TEST_ACCESS_TOKEN);
			given(jwtTokenProvider.generateRefreshToken(anyLong())).willReturn(TEST_REFRESH_TOKEN);
			given(jwtTokenProvider.getAccessTokenExpiresIn()).willReturn(86400L);
			given(jwtTokenProvider.getRefreshTokenExpiresIn()).willReturn(1209600000L);

			// when
			authService.login("auth-code");

			// then
			verify(refreshTokenRepository, times(1)).save(any(), anyLong());
		}

		@Test
		@DisplayName("should include kakao user info in login response")
		void shouldIncludeKakaoUserInfoInLoginResponse() {
			// given
			setupTokenWebClientMock(Mono.just(createTokenResponse()));
			setupUserInfoWebClientMock(Mono.just(createKakaoUserInfo(TEST_SOCIAL_ID, TEST_EMAIL)));

			given(memberService.findBySocialId(SnsProvider.KAKAO, TEST_SOCIAL_ID)).willReturn(
				Optional.of(activeMember));
			given(jwtTokenProvider.generateAccessToken(anyLong(), anyString(), anyString())).willReturn(
				TEST_ACCESS_TOKEN);
			given(jwtTokenProvider.generateRefreshToken(anyLong())).willReturn(TEST_REFRESH_TOKEN);
			given(jwtTokenProvider.getAccessTokenExpiresIn()).willReturn(86400L);

			// when
			LoginResponse response = authService.login("auth-code");

			// then
			assertSoftly(softly -> {
				softly.assertThat(response.getKakaoId()).isEqualTo(TEST_SOCIAL_ID);
				softly.assertThat(response.getEmail()).isEqualTo(TEST_EMAIL);
				softly.assertThat(response.getConnectedAt()).isNotNull();
			});
		}

		@Test
		@DisplayName("should create member without email when kakao account has no email")
		void shouldCreateMemberWithoutEmailWhenKakaoAccountHasNoEmail() {
			// given
			setupTokenWebClientMock(Mono.just(createTokenResponse()));
			setupUserInfoWebClientMock(Mono.just(createKakaoUserInfo(TEST_SOCIAL_ID, null)));

			given(memberService.findBySocialId(SnsProvider.KAKAO, TEST_SOCIAL_ID)).willReturn(Optional.empty());
			given(memberService.createdFromSnsUser(any())).willReturn(inactiveMember);
			given(jwtTokenProvider.generateAccessToken(anyLong(), anyString(), anyString())).willReturn(
				TEST_ACCESS_TOKEN);
			given(jwtTokenProvider.generateRefreshToken(anyLong())).willReturn(TEST_REFRESH_TOKEN);
			given(jwtTokenProvider.getAccessTokenExpiresIn()).willReturn(86400L);

			// when
			LoginResponse response = authService.login("auth-code");

			// then
			assertThat(response.getIsNewMember()).isTrue();
			verify(memberService, times(1)).createdFromSnsUser(any());
		}

		// --- getAccessToken 내부 분기 ---

		@ParameterizedTest(name = "should throw INVALID_TOKEN_VALUE when kakao token api returns 400 for code=\"{0}\"")
		@DisplayName("should throw INVALID_TOKEN_VALUE when kakao token api returns 400")
		@ValueSource(strings = {"expired-code", "already-used-code", ""})
		void shouldThrowInvalidTokenValueWhenKakaoTokenApiReturns400(String invalidCode) {
			// given
			setupTokenWebClientMock(Mono.error(makeWebClientException(HttpStatus.BAD_REQUEST)));

			// when & then
			assertThatThrownBy(() -> authService.login(invalidCode))
				.isInstanceOf(MemberServiceApiException.class)
				.satisfies(ex -> assertThat(((MemberServiceApiException)ex).getErrorCode())
					.isEqualTo(ErrorCode.INVALID_TOKEN_VALUE));
		}

		@Test
		@DisplayName("should throw INVALID_TOKEN when kakao token api returns 401")
		void shouldThrowInvalidTokenWhenKakaoTokenApiReturns401() {
			// given
			setupTokenWebClientMock(Mono.error(makeWebClientException(HttpStatus.UNAUTHORIZED)));

			// when & then
			assertThatThrownBy(() -> authService.login("any-code"))
				.isInstanceOf(MemberServiceApiException.class)
				.satisfies(ex -> assertThat(((MemberServiceApiException)ex).getErrorCode())
					.isEqualTo(ErrorCode.INVALID_TOKEN));
		}

		@Test
		@DisplayName("should throw EXTERNAL_API_ERROR when kakao token api returns 500")
		void shouldThrowExternalApiErrorWhenKakaoTokenApiReturns500() {
			// given
			setupTokenWebClientMock(Mono.error(makeWebClientException(HttpStatus.INTERNAL_SERVER_ERROR)));

			// when & then
			assertThatThrownBy(() -> authService.login("any-code"))
				.isInstanceOf(MemberServiceApiException.class)
				.satisfies(ex -> assertThat(((MemberServiceApiException)ex).getErrorCode())
					.isEqualTo(ErrorCode.EXTERNAL_API_ERROR));
		}

		@Test
		@DisplayName("should throw EXTERNAL_API_ERROR when kakao token response is null")
		void shouldThrowExternalApiErrorWhenKakaoTokenResponseIsNull() {
			// given
			setupTokenWebClientMock(Mono.justOrEmpty(null));

			// when & then
			assertThatThrownBy(() -> authService.login("any-code"))
				.isInstanceOf(MemberServiceApiException.class)
				.satisfies(ex -> assertThat(((MemberServiceApiException)ex).getErrorCode())
					.isEqualTo(ErrorCode.EXTERNAL_API_ERROR));
		}

		@Test
		@DisplayName("should throw EXTERNAL_API_ERROR when unexpected exception occurs during token request")
		void shouldThrowExternalApiErrorWhenUnexpectedExceptionOccursDuringTokenRequest() {
			// given
			setupTokenWebClientMock(Mono.error(new RuntimeException("네트워크 오류")));

			// when & then
			assertThatThrownBy(() -> authService.login("any-code"))
				.isInstanceOf(MemberServiceApiException.class)
				.satisfies(ex -> assertThat(((MemberServiceApiException)ex).getErrorCode())
					.isEqualTo(ErrorCode.EXTERNAL_API_ERROR));
		}

		// --- getKakaoUserInfo 내부 분기 ---

		@Test
		@DisplayName("should throw INVALID_TOKEN when kakao user-info api returns 401")
		void shouldThrowInvalidTokenWhenKakaoUserInfoApiReturns401() {
			// given
			setupTokenWebClientMock(Mono.just(createTokenResponse()));
			setupUserInfoWebClientMock(Mono.error(makeWebClientException(HttpStatus.UNAUTHORIZED)));

			// when & then
			assertThatThrownBy(() -> authService.login("any-code"))
				.isInstanceOf(MemberServiceApiException.class)
				.satisfies(ex -> assertThat(((MemberServiceApiException)ex).getErrorCode())
					.isEqualTo(ErrorCode.INVALID_TOKEN));
		}

		@Test
		@DisplayName("should throw EXTERNAL_API_ERROR when kakao user-info api returns 500")
		void shouldThrowExternalApiErrorWhenKakaoUserInfoApiReturns500() {
			// given
			setupTokenWebClientMock(Mono.just(createTokenResponse()));
			setupUserInfoWebClientMock(Mono.error(makeWebClientException(HttpStatus.INTERNAL_SERVER_ERROR)));

			// when & then
			assertThatThrownBy(() -> authService.login("any-code"))
				.isInstanceOf(MemberServiceApiException.class)
				.satisfies(ex -> assertThat(((MemberServiceApiException)ex).getErrorCode())
					.isEqualTo(ErrorCode.EXTERNAL_API_ERROR));
		}

		@Test
		@DisplayName("should throw EXTERNAL_API_ERROR when kakao user-info response is null")
		void shouldThrowExternalApiErrorWhenKakaoUserInfoResponseIsNull() {
			// given
			setupTokenWebClientMock(Mono.just(createTokenResponse()));
			setupUserInfoWebClientMock(Mono.justOrEmpty(null));

			// when & then
			assertThatThrownBy(() -> authService.login("any-code"))
				.isInstanceOf(MemberServiceApiException.class)
				.satisfies(ex -> assertThat(((MemberServiceApiException)ex).getErrorCode())
					.isEqualTo(ErrorCode.EXTERNAL_API_ERROR));
		}

		@Test
		@DisplayName("should throw EXTERNAL_API_ERROR when unexpected exception occurs during user-info request")
		void shouldThrowExternalApiErrorWhenUnexpectedExceptionOccursDuringUserInfoRequest() {
			// given
			setupTokenWebClientMock(Mono.just(createTokenResponse()));
			setupUserInfoWebClientMock(Mono.error(new RuntimeException("타임아웃")));

			// when & then
			assertThatThrownBy(() -> authService.login("any-code"))
				.isInstanceOf(MemberServiceApiException.class)
				.satisfies(ex -> assertThat(((MemberServiceApiException)ex).getErrorCode())
					.isEqualTo(ErrorCode.EXTERNAL_API_ERROR));
		}
	}

	// =========================================================
	// refreshAccessToken
	// =========================================================
	@Nested
	@DisplayName("refreshAccessToken")
	class RefreshAccessToken {

		@Test
		@DisplayName("should return new access token when refresh token is valid")
		void shouldReturnNewAccessTokenWhenRefreshTokenIsValid() {
			// given
			given(jwtTokenProvider.validateToken(TEST_REFRESH_TOKEN)).willReturn(true);
			given(jwtTokenProvider.getMemberIdFromToken(TEST_REFRESH_TOKEN)).willReturn(TEST_MEMBER_ID);
			given(refreshTokenRepository.findByMemberId(TEST_MEMBER_ID)).willReturn(Optional.of(TEST_REFRESH_TOKEN));
			given(memberService.findById(TEST_MEMBER_ID)).willReturn(activeMember);
			given(jwtTokenProvider.generateAccessToken(TEST_MEMBER_ID, TEST_SOCIAL_ID, "KAKAO")).willReturn(
				"new-access-token");
			given(jwtTokenProvider.getAccessTokenExpiresIn()).willReturn(86400L);

			// when
			TokenRefreshResponse response = authService.refreshAccessToken(TEST_REFRESH_TOKEN);

			// then
			assertSoftly(softly -> {
				softly.assertThat(response.getTokenType()).isEqualTo("Bearer");
				softly.assertThat(response.getAccessToken()).isEqualTo("new-access-token");
				softly.assertThat(response.getExpiresIn()).isEqualTo(86400L);
			});
		}

		@Test
		@DisplayName("should throw INVALID_TOKEN when refresh token fails validation")
		void shouldThrowInvalidTokenWhenRefreshTokenFailsValidation() {
			// given
			given(jwtTokenProvider.validateToken("invalid-token")).willReturn(false);

			// when & then
			assertThatThrownBy(() -> authService.refreshAccessToken("invalid-token"))
				.isInstanceOf(MemberServiceApiException.class)
				.satisfies(ex -> assertThat(((MemberServiceApiException)ex).getErrorCode())
					.isEqualTo(ErrorCode.INVALID_TOKEN));
		}

		@ParameterizedTest(name = "should throw INVALID_TOKEN for malformed token=\"{0}\"")
		@DisplayName("should throw INVALID_TOKEN for malformed refresh tokens")
		@ValueSource(strings = {"", "not.a.jwt", "Bearer only", "eyJhbGciOiJIUzI1NiJ9"})
		void shouldThrowInvalidTokenForMalformedRefreshTokens(String malformedToken) {
			// given
			given(jwtTokenProvider.validateToken(malformedToken)).willReturn(false);

			// when & then
			assertThatThrownBy(() -> authService.refreshAccessToken(malformedToken))
				.isInstanceOf(MemberServiceApiException.class)
				.satisfies(ex -> assertThat(((MemberServiceApiException)ex).getErrorCode())
					.isEqualTo(ErrorCode.INVALID_TOKEN));
		}

		@Test
		@DisplayName("should throw INVALID_TOKEN when no refresh token stored in redis")
		void shouldThrowInvalidTokenWhenNoRefreshTokenStoredInRedis() {
			// given
			given(jwtTokenProvider.validateToken(TEST_REFRESH_TOKEN)).willReturn(true);
			given(jwtTokenProvider.getMemberIdFromToken(TEST_REFRESH_TOKEN)).willReturn(TEST_MEMBER_ID);
			given(refreshTokenRepository.findByMemberId(TEST_MEMBER_ID)).willReturn(Optional.empty());

			// when & then
			assertThatThrownBy(() -> authService.refreshAccessToken(TEST_REFRESH_TOKEN))
				.isInstanceOf(MemberServiceApiException.class)
				.satisfies(ex -> assertThat(((MemberServiceApiException)ex).getErrorCode())
					.isEqualTo(ErrorCode.INVALID_TOKEN));
		}

		@Test
		@DisplayName("should throw INVALID_TOKEN when stored token does not match request token")
		void shouldThrowInvalidTokenWhenStoredTokenDoesNotMatchRequestToken() {
			// given
			given(jwtTokenProvider.validateToken(TEST_REFRESH_TOKEN)).willReturn(true);
			given(jwtTokenProvider.getMemberIdFromToken(TEST_REFRESH_TOKEN)).willReturn(TEST_MEMBER_ID);
			given(refreshTokenRepository.findByMemberId(TEST_MEMBER_ID)).willReturn(
				Optional.of("completely-different-token"));

			// when & then
			assertThatThrownBy(() -> authService.refreshAccessToken(TEST_REFRESH_TOKEN))
				.isInstanceOf(MemberServiceApiException.class)
				.satisfies(ex -> assertThat(((MemberServiceApiException)ex).getErrorCode())
					.isEqualTo(ErrorCode.INVALID_TOKEN));
		}

		@Test
		@DisplayName("should throw exception when member not found during token refresh")
		void shouldThrowExceptionWhenMemberNotFoundDuringTokenRefresh() {
			// given
			given(jwtTokenProvider.validateToken(TEST_REFRESH_TOKEN)).willReturn(true);
			given(jwtTokenProvider.getMemberIdFromToken(TEST_REFRESH_TOKEN)).willReturn(TEST_MEMBER_ID);
			given(refreshTokenRepository.findByMemberId(TEST_MEMBER_ID)).willReturn(Optional.of(TEST_REFRESH_TOKEN));
			given(memberService.findById(TEST_MEMBER_ID))
				.willThrow(new MemberServiceApiException(ErrorCode.MEMBER_NOT_FOUND));

			// when & then
			assertThatThrownBy(() -> authService.refreshAccessToken(TEST_REFRESH_TOKEN))
				.isInstanceOf(MemberServiceApiException.class)
				.satisfies(ex -> assertThat(((MemberServiceApiException)ex).getErrorCode())
					.isEqualTo(ErrorCode.MEMBER_NOT_FOUND));
		}

		@Test
		@DisplayName("should not generate new access token when token validation fails")
		void shouldNotGenerateNewAccessTokenWhenTokenValidationFails() {
			// given
			given(jwtTokenProvider.validateToken("bad-token")).willReturn(false);

			// when & then
			assertThatThrownBy(() -> authService.refreshAccessToken("bad-token"))
				.isInstanceOf(MemberServiceApiException.class);

			verify(jwtTokenProvider, never()).generateAccessToken(anyLong(), anyString(), anyString());
		}
	}
}