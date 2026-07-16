package com.yeolcheong.mub.member.service;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.SoftAssertions.*;
import static org.mockito.ArgumentMatchers.*;
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

import com.yeolcheong.mub.member.client.KakaoClient;
import com.yeolcheong.mub.member.domain.Member;
import com.yeolcheong.mub.member.domain.MemberStatus;
import com.yeolcheong.mub.member.domain.SnsProvider;
import com.yeolcheong.mub.member.dto.LoginResponse;
import com.yeolcheong.mub.member.dto.LoginTokenResponse;
import com.yeolcheong.mub.member.dto.SnsUserInfoResponse;
import com.yeolcheong.mub.member.dto.TokenRefreshResponse;
import com.yeolcheong.mub.member.exception.ErrorCode;
import com.yeolcheong.mub.member.exception.MemberServiceApiException;
import com.yeolcheong.mub.member.repository.RefreshTokenRepository;
import com.yeolcheong.mub.member.security.JwtTokenProvider;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService")
class AuthServiceTest {

	private static final Long TEST_MEMBER_ID = 1L;
	private static final String TEST_SOCIAL_ID = "1212343456";
	private static final String TEST_EMAIL = "kakaoLoginTest@example.com";
	private static final String TEST_ACCESS_TOKEN = "jwt-access-token";
	private static final String TEST_REFRESH_TOKEN = "jwt-refresh-token";

	@Mock
	private KakaoClient kakaoClient;
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

	// =========================================================
	// getAuthUrl
	// =========================================================
	@Nested
	@DisplayName("getAuthUrl")
	class GetAuthUrl {

		@Test
		@DisplayName("should delegate to KakaoClient.buildAuthorizationUrl")
		void shouldDelegateToKakaoClientBuildAuthorizationUrl() {
			// given
			String expected = "https://kauth.kakao.com/oauth/authorize?client_id=cid&redirect_uri=ru&response_type=code";
			given(kakaoClient.buildAuthorizationUrl()).willReturn(expected);

			// when
			String authUrl = authService.getAuthUrl();

			// then
			assertThat(authUrl).isEqualTo(expected);
			then(kakaoClient).should().buildAuthorizationUrl();
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
			stubKakaoLoginFlow(TEST_SOCIAL_ID, TEST_EMAIL);
			given(memberService.findBySocialId(SnsProvider.KAKAO, TEST_SOCIAL_ID)).willReturn(Optional.empty());
			given(memberService.createdFromSnsUser(any())).willReturn(inactiveMember);
			stubJwtIssuance();

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
			then(memberService).should(times(1)).createdFromSnsUser(any());
		}

		@Test
		@DisplayName("should skip member creation and return isNewMember=false on existing member login")
		void shouldSkipMemberCreationAndReturnIsNewMemberFalseOnExistingMemberLogin() {
			// given
			stubKakaoLoginFlow(TEST_SOCIAL_ID, TEST_EMAIL);
			given(memberService.findBySocialId(SnsProvider.KAKAO, TEST_SOCIAL_ID)).willReturn(Optional.of(activeMember));
			stubJwtIssuance();

			// when
			LoginResponse response = authService.login("auth-code");

			// then
			assertThat(response.getIsNewMember()).isFalse();
			then(memberService).should(never()).createdFromSnsUser(any());
		}

		@Test
		@DisplayName("should reactivate withdrawn member and return isNewMember=true on re-signup")
		void shouldReactivateWithdrawnMemberOnResignup() {
			// given — 같은 socialId 의 탈퇴(DELETED) 회원이 다시 로그인
			Member withdrawnMember = Member.builder()
				.id(TEST_MEMBER_ID)
				.snsProvider(SnsProvider.KAKAO)
				.socialId(TEST_SOCIAL_ID)
				.email(TEST_EMAIL)
				.status(MemberStatus.DELETED)
				.build();
			stubKakaoLoginFlow(TEST_SOCIAL_ID, TEST_EMAIL);
			given(memberService.findBySocialId(SnsProvider.KAKAO, TEST_SOCIAL_ID))
				.willReturn(Optional.of(withdrawnMember));
			stubJwtIssuance();

			// when
			LoginResponse response = authService.login("auth-code");

			// then
			assertThat(response.getIsNewMember()).isTrue();
			then(memberService).should(times(1)).reactivateForResignup(withdrawnMember);
			then(memberService).should(never()).createdFromSnsUser(any());
		}

		@Test
		@DisplayName("should save refresh token to redis on login")
		void shouldSaveRefreshTokenToRedisOnLogin() {
			// given
			stubKakaoLoginFlow(TEST_SOCIAL_ID, TEST_EMAIL);
			given(memberService.findBySocialId(SnsProvider.KAKAO, TEST_SOCIAL_ID)).willReturn(Optional.of(activeMember));
			stubJwtIssuance();
			given(jwtTokenProvider.getRefreshTokenExpiresIn()).willReturn(1209600000L);

			// when
			authService.login("auth-code");

			// then
			then(refreshTokenRepository).should(times(1)).save(any(), anyLong());
		}

		@Test
		@DisplayName("should include kakao user info in login response")
		void shouldIncludeKakaoUserInfoInLoginResponse() {
			// given
			stubKakaoLoginFlow(TEST_SOCIAL_ID, TEST_EMAIL);
			given(memberService.findBySocialId(SnsProvider.KAKAO, TEST_SOCIAL_ID)).willReturn(Optional.of(activeMember));
			stubJwtIssuance();

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
			stubKakaoLoginFlow(TEST_SOCIAL_ID, null);
			given(memberService.findBySocialId(SnsProvider.KAKAO, TEST_SOCIAL_ID)).willReturn(Optional.empty());
			given(memberService.createdFromSnsUser(any())).willReturn(inactiveMember);
			stubJwtIssuance();

			// when
			LoginResponse response = authService.login("auth-code");

			// then
			assertThat(response.getIsNewMember()).isTrue();
			then(memberService).should(times(1)).createdFromSnsUser(any());
		}

		@Test
		@DisplayName("should propagate exception when KakaoClient.fetchAccessToken fails")
		void shouldPropagateExceptionWhenFetchAccessTokenFails() {
			// given
			given(kakaoClient.fetchAccessToken(anyString()))
				.willThrow(new MemberServiceApiException(ErrorCode.INVALID_TOKEN_VALUE));

			// when & then
			assertThatThrownBy(() -> authService.login("any-code"))
				.isInstanceOf(MemberServiceApiException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.INVALID_TOKEN_VALUE);
			then(kakaoClient).should(never()).fetchUserInfo(anyString());
		}

		@Test
		@DisplayName("should propagate exception when KakaoClient.fetchUserInfo fails")
		void shouldPropagateExceptionWhenFetchUserInfoFails() {
			// given
			given(kakaoClient.fetchAccessToken(anyString())).willReturn(buildTokenResponse());
			given(kakaoClient.fetchUserInfo(anyString()))
				.willThrow(new MemberServiceApiException(ErrorCode.INVALID_TOKEN));

			// when & then
			assertThatThrownBy(() -> authService.login("any-code"))
				.isInstanceOf(MemberServiceApiException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.INVALID_TOKEN);
			then(memberService).should(never()).findBySocialId(any(), anyString());
		}
	}

	// =========================================================
	// loginWithKakaoToken
	// =========================================================
	@Nested
	@DisplayName("loginWithKakaoToken")
	class LoginWithKakaoToken {

		@Test
		@DisplayName("should issue tokens for existing member when called with kakao access token")
		void shouldIssueTokensForExistingMemberWhenCalledWithKakaoAccessToken() {
			// given
			given(kakaoClient.fetchUserInfo("kakao-sdk-token")).willReturn(buildUserInfo(TEST_SOCIAL_ID, TEST_EMAIL));
			given(memberService.findBySocialId(SnsProvider.KAKAO, TEST_SOCIAL_ID)).willReturn(Optional.of(activeMember));
			stubJwtIssuance();

			// when
			LoginResponse response = authService.loginWithKakaoToken("kakao-sdk-token");

			// then
			assertSoftly(softly -> {
				softly.assertThat(response.getIsNewMember()).isFalse();
				softly.assertThat(response.getAccessToken()).isEqualTo(TEST_ACCESS_TOKEN);
			});
			then(kakaoClient).should(never()).fetchAccessToken(anyString());
		}

		@Test
		@DisplayName("should create new member when kakao access token is for unknown user")
		void shouldCreateNewMemberWhenKakaoAccessTokenIsForUnknownUser() {
			// given
			given(kakaoClient.fetchUserInfo(anyString())).willReturn(buildUserInfo(TEST_SOCIAL_ID, TEST_EMAIL));
			given(memberService.findBySocialId(SnsProvider.KAKAO, TEST_SOCIAL_ID)).willReturn(Optional.empty());
			given(memberService.createdFromSnsUser(any())).willReturn(inactiveMember);
			stubJwtIssuance();

			// when
			LoginResponse response = authService.loginWithKakaoToken("kakao-sdk-token");

			// then
			assertThat(response.getIsNewMember()).isTrue();
			then(memberService).should(times(1)).createdFromSnsUser(any());
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
			given(jwtTokenProvider.generateAccessToken(TEST_MEMBER_ID, TEST_SOCIAL_ID, "KAKAO"))
				.willReturn("new-access-token");
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
				.extracting("errorCode")
				.isEqualTo(ErrorCode.INVALID_TOKEN);
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
				.extracting("errorCode")
				.isEqualTo(ErrorCode.INVALID_TOKEN);
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
				.extracting("errorCode")
				.isEqualTo(ErrorCode.INVALID_TOKEN);
		}

		@Test
		@DisplayName("should throw INVALID_TOKEN when stored token does not match request token")
		void shouldThrowInvalidTokenWhenStoredTokenDoesNotMatchRequestToken() {
			// given
			given(jwtTokenProvider.validateToken(TEST_REFRESH_TOKEN)).willReturn(true);
			given(jwtTokenProvider.getMemberIdFromToken(TEST_REFRESH_TOKEN)).willReturn(TEST_MEMBER_ID);
			given(refreshTokenRepository.findByMemberId(TEST_MEMBER_ID))
				.willReturn(Optional.of("completely-different-token"));

			// when & then
			assertThatThrownBy(() -> authService.refreshAccessToken(TEST_REFRESH_TOKEN))
				.isInstanceOf(MemberServiceApiException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.INVALID_TOKEN);
		}

		@Test
		@DisplayName("should propagate exception when member not found during token refresh")
		void shouldPropagateExceptionWhenMemberNotFoundDuringTokenRefresh() {
			// given
			given(jwtTokenProvider.validateToken(TEST_REFRESH_TOKEN)).willReturn(true);
			given(jwtTokenProvider.getMemberIdFromToken(TEST_REFRESH_TOKEN)).willReturn(TEST_MEMBER_ID);
			given(refreshTokenRepository.findByMemberId(TEST_MEMBER_ID)).willReturn(Optional.of(TEST_REFRESH_TOKEN));
			given(memberService.findById(TEST_MEMBER_ID))
				.willThrow(new MemberServiceApiException(ErrorCode.MEMBER_NOT_FOUND));

			// when & then
			assertThatThrownBy(() -> authService.refreshAccessToken(TEST_REFRESH_TOKEN))
				.isInstanceOf(MemberServiceApiException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.MEMBER_NOT_FOUND);
		}

		@Test
		@DisplayName("should not generate new access token when token validation fails")
		void shouldNotGenerateNewAccessTokenWhenTokenValidationFails() {
			// given
			given(jwtTokenProvider.validateToken("bad-token")).willReturn(false);

			// when & then
			assertThatThrownBy(() -> authService.refreshAccessToken("bad-token"))
				.isInstanceOf(MemberServiceApiException.class);

			then(jwtTokenProvider).should(never()).generateAccessToken(anyLong(), anyString(), anyString());
		}
	}

	@Nested
	@DisplayName("logout")
	class Logout {

		@Test
		@DisplayName("should invalidate the stored refresh token")
		void logout_deletesRefreshToken() {
			// when
			authService.logout(TEST_MEMBER_ID);

			// then
			then(refreshTokenRepository).should(times(1)).deleteByMemberId(TEST_MEMBER_ID);
		}
	}

	// =========================================================
	// Helpers
	// =========================================================

	private void stubKakaoLoginFlow(String socialId, String email) {
		given(kakaoClient.fetchAccessToken(anyString())).willReturn(buildTokenResponse());
		given(kakaoClient.fetchUserInfo(anyString())).willReturn(buildUserInfo(socialId, email));
	}

	private void stubJwtIssuance() {
		given(jwtTokenProvider.generateAccessToken(anyLong(), anyString(), anyString())).willReturn(TEST_ACCESS_TOKEN);
		given(jwtTokenProvider.generateRefreshToken(anyLong())).willReturn(TEST_REFRESH_TOKEN);
		given(jwtTokenProvider.getAccessTokenExpiresIn()).willReturn(86400L);
	}

	private LoginTokenResponse buildTokenResponse() {
		return LoginTokenResponse.builder()
			.tokenType("Bearer")
			.accessToken("kakao-access-token")
			.expiresIn(21599)
			.refreshToken("kakao-refresh-token")
			.refreshTokenExpiresIn(5183999)
			.build();
	}

	private SnsUserInfoResponse buildUserInfo(String socialId, String email) {
		return SnsUserInfoResponse.builder()
			.id(Long.parseLong(socialId))
			.connectedAt("2025-10-11T09:00:00Z")
			.kakaoAccount(SnsUserInfoResponse.KakaoAccount.builder().email(email).build())
			.build();
	}
}
