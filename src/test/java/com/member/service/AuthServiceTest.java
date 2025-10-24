package com.member.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.member.common.KakaoProperties;
import com.member.common.MemberStatus;
import com.member.common.SnsProvider;
import com.member.domain.Member;
import com.member.dto.LoginResponse;
import com.member.dto.LoginTokenResponse;
import com.member.dto.SnsUserInfoResponse;
import com.member.dto.TokenRefreshResponse;
import com.member.exception.ErrorCode;
import com.member.exception.MemberServiceApiException;
import com.member.repository.RefreshTokenRepository;
import com.member.security.JwtTokenProvider;

import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AuthService 단위 테스트")
class AuthServiceTest {

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

	private Member testMember;

	private static final Long TEST_MEMBER_ID = 1L;
	private static final String TEST_KAKAO_ID = "1212343456";
	private static final String TEST_KAKAO_EMAIL = "kakaoLoginTest@example.com";
	private static final String TEST_ACCESS_TOKEN = "jwt-access-token";
	private static final String TEST_REFRESH_TOKEN = "jwt-refresh-token";

	@BeforeEach
	void setUp() {
		given(kakaoProperties.getAuthUrl()).willReturn("https://kauth.kakao.com/oauth/authorize/");
		given(kakaoProperties.getClientId()).willReturn("test-client-id");
		given(kakaoProperties.getRedirectUri()).willReturn("http://localhost:8081/api/auth/kakao/callback");
		given(kakaoProperties.getTokenUrl()).willReturn("https://kauth.kakao.com/oauth/token");
		given(kakaoProperties.getUserInfoUrl()).willReturn("https://kapi.kakao.com/v2/user/me");

		testMember = Member.builder()
			.id(1L)
			.snsProvider(SnsProvider.KAKAO)
			.socialId("1234567890")
			.email("test@example.com")
			.status(MemberStatus.ACTIVE)
			.build();
	}

	@Test
	@DisplayName("카카오 인가를 위한 URL을 정확하게 생성하여 리턴한다")
	void shouldGenerateCorrectAuthUrl() {
		// when
		String authUrl = authService.getAuthUrl();

		System.out.println("authUrl=" + authUrl);

		// then
		assertThat(authUrl).isNotNull();
		assertThat(authUrl).contains("https://kauth.kakao.com/oauth/authorize");
		assertThat(authUrl).contains("client_id=test-client-id");
		assertThat(authUrl).contains("http://localhost:8081/api/auth/kakao/callback");
		assertThat(authUrl).contains("response_type=code");
	}

	@Test
	@DisplayName("생성된 URL이 필수 파라미터를 모두 포함한다")
	void shouldContainAllRequiredParameters() {
		// when
		String authUrl = authService.getAuthUrl();

		// then
		assertThat(authUrl)
			.contains("client_id=")
			.contains("redirect_uri=")
			.contains("response_type=code");
	}

	@Test
	@DisplayName("신규 회원 로그인 시, 소셜로그인 정보를 바탕으로 회원과 토큰을 정상 생성한다")
	void shouldNewMemberLoginSuccessfully() {
		// given
		String authorizationCode = "test-authorization-code";

		LoginTokenResponse loginTokenMockResponse = createTokenMockResponse();
		SnsUserInfoResponse kakaoUserInfoMockResponse = createKakaoUserInfoMockResponse();

		Member newMember = Member.builder()
			.id(TEST_MEMBER_ID)
			.snsProvider(SnsProvider.KAKAO)
			.socialId(TEST_KAKAO_ID)
			.email(TEST_KAKAO_EMAIL)
			.status(MemberStatus.INACTIVE)
			.lastLoginAt(LocalDateTime.now())
			.build();

		given(memberService.findBySocialId(SnsProvider.KAKAO, TEST_KAKAO_ID)).willReturn(java.util.Optional.empty());
		given(memberService.createdFromSnsUser(any(SnsUserInfoResponse.class))).willReturn(newMember);

		given(jwtTokenProvider.generateAccessToken(anyLong(), anyString(), anyString())).willReturn(
			TEST_ACCESS_TOKEN);
		given(jwtTokenProvider.generateRefreshToken(anyLong())).willReturn(TEST_REFRESH_TOKEN);
		given(jwtTokenProvider.getAccessTokenExpiresIn()).willReturn(86400L);

		setupWebClientMocks(loginTokenMockResponse, kakaoUserInfoMockResponse);

		LoginResponse response = authService.login(authorizationCode);

		assertThat(response).isNotNull();
		assertThat(response.getTokenType()).isEqualTo("Bearer");
		assertThat(response.getAccessToken()).isEqualTo(TEST_ACCESS_TOKEN);
		assertThat(response.getExpiresIn()).isEqualTo(86400L);
		assertThat(response.getRefreshToken()).isEqualTo(TEST_REFRESH_TOKEN);

		assertThat(response.getKakaoId()).isEqualTo(TEST_KAKAO_ID);
		assertThat(response.getEmail()).isEqualTo(TEST_KAKAO_EMAIL);
		assertThat(response.getConnectedAt()).isNotNull();

		assertThat(response.getIsNewMember()).isTrue();

		verify(memberService).createdFromSnsUser(any(SnsUserInfoResponse.class));
	}

	@Test
	@DisplayName("카카오 토큰 발급 실패 시 예외를 던진다")
	void shouldThrowExceptionWhenSocialTokenFails() {
		// given
		String authorizationCode = "invalid-code";

		given(webClient.post()).willReturn(requestBodyUriSpec);
		given(requestBodyUriSpec.uri(anyString())).willReturn(requestBodySpec);
		given(requestBodySpec.contentType(any())).willReturn(requestBodySpec);
		given(requestBodySpec.bodyValue(any())).willAnswer(invocation -> requestHeadersSpec);
		given(requestHeadersSpec.retrieve()).willReturn(responseSpec);
		given(responseSpec.bodyToMono(LoginTokenResponse.class))
			.willReturn(Mono.error(WebClientResponseException.create(
				HttpStatus.BAD_REQUEST.value(),
				"Bad Request",
				null,
				"{\"error\":\"invalid_grant\"}".getBytes(),
				null
			)));

		// when & then
		assertThatThrownBy(() -> authService.login(authorizationCode))
			.isInstanceOf(MemberServiceApiException.class)
			.hasMessageContaining(ErrorCode.INVALID_TOKEN_VALUE.getMessage());
	}

	@Test
	@DisplayName("카카오 사용자 정보 조회 실패 시 예외를 던진다")
	void shouldThrowExceptionWhenUserInfoFails() {
		// given
		String authorizationCode = "test-code";
		LoginTokenResponse loginTokenResponse = createTokenMockResponse();

		given(webClient.post()).willReturn(requestBodyUriSpec);
		given(requestBodyUriSpec.uri(anyString())).willReturn(requestBodySpec);
		given(requestBodySpec.contentType(any())).willReturn(requestBodySpec);
		given(requestBodySpec.bodyValue(any())).willAnswer(invocation -> requestHeadersSpec);
		given(requestHeadersSpec.retrieve()).willReturn(responseSpec);
		given(responseSpec.bodyToMono(LoginTokenResponse.class))
			.willReturn(Mono.just(loginTokenResponse));

		given(webClient.get()).willAnswer(invocation -> requestHeadersUriSpec);
		given(requestHeadersUriSpec.uri(anyString())).willAnswer(invocation -> requestHeadersUriSpec);
		given(requestHeadersUriSpec.header(anyString(), anyString())).willAnswer(invocation -> requestHeadersUriSpec);
		given(requestHeadersUriSpec.retrieve()).willReturn(responseSpec);
		given(responseSpec.bodyToMono(SnsUserInfoResponse.class))
			.willReturn(Mono.error(WebClientResponseException.create(
				HttpStatus.UNAUTHORIZED.value(),
				"Unauthorized",
				null,
				null,
				null
			)));

		// when & then
		assertThatThrownBy(() -> authService.login(authorizationCode))
			.isInstanceOf(MemberServiceApiException.class)
			.hasMessageContaining("유효하지 않은 카카오 토큰입니다");
	}

	@Test
	@DisplayName("유효한 Refresh Token으로 Access Token 재발급 성공")
	void shouldRefreshAccessTokenSuccessfully() {
		// given
		given(jwtTokenProvider.validateToken(TEST_REFRESH_TOKEN)).willReturn(true);
		given(jwtTokenProvider.getMemberIdFromToken(TEST_REFRESH_TOKEN)).willReturn(1L);
		given(refreshTokenRepository.findByMemberId(1L)).willReturn(Optional.of(TEST_REFRESH_TOKEN));
		given(memberService.findById(1L)).willReturn(testMember);
		given(jwtTokenProvider.generateAccessToken(1L, "1234567890", "KAKAO"))
			.willReturn("new-access-token");
		given(jwtTokenProvider.getAccessTokenExpiresIn()).willReturn(86400L);

		// when
		TokenRefreshResponse response = authService.refreshAccessToken(TEST_REFRESH_TOKEN);

		// then
		assertThat(response).isNotNull();
		assertThat(response.getTokenType()).isEqualTo("Bearer");
		assertThat(response.getAccessToken()).isEqualTo("new-access-token");
		assertThat(response.getExpiresIn()).isEqualTo(86400L);
	}

	@Test
	@DisplayName("유효하지 않은 Refresh Token으로 재발급 실패")
	void shouldFailToRefreshWithInvalidToken() {
		// given
		String invalidToken = "invalid-token";
		given(jwtTokenProvider.validateToken(invalidToken)).willReturn(false);

		// when & then
		assertThatThrownBy(() -> authService.refreshAccessToken(invalidToken))
			.isInstanceOf(MemberServiceApiException.class)
			.hasMessageContaining("유효하지 않은 Refresh Token입니다");
	}

	@Test
	@DisplayName("Redis에 저장된 토큰과 불일치하면 재발급 실패")
	void shouldFailToRefreshWithMismatchedToken() {
		// given
		given(jwtTokenProvider.validateToken(TEST_REFRESH_TOKEN)).willReturn(true);
		given(jwtTokenProvider.getMemberIdFromToken(TEST_REFRESH_TOKEN)).willReturn(1L);
		given(refreshTokenRepository.findByMemberId(1L)).willReturn(Optional.of("different-token"));

		// when & then
		assertThatThrownBy(() -> authService.refreshAccessToken(TEST_REFRESH_TOKEN))
			.isInstanceOf(MemberServiceApiException.class)
			.hasMessageContaining("유효하지 않은 Refresh Token입니다");
	}

	private void setupWebClientMocks(LoginTokenResponse loginTokenMockResponse,
		SnsUserInfoResponse kakaoUserInfoMockResponse) {

		given(webClient.post()).willReturn(requestBodyUriSpec);
		given(requestBodyUriSpec.uri(anyString())).willReturn(requestBodySpec);
		given(requestBodySpec.contentType(any())).willReturn(requestBodySpec);
		given(requestBodySpec.bodyValue(any())).willAnswer(invocation -> requestHeadersSpec);
		given(requestHeadersSpec.retrieve()).willReturn(responseSpec);
		given(responseSpec.bodyToMono(LoginTokenResponse.class))
			.willReturn(Mono.just(loginTokenMockResponse));

		given(webClient.get()).willAnswer(invocation -> requestHeadersUriSpec);
		given(requestHeadersUriSpec.uri(anyString())).willAnswer(invocation -> requestHeadersUriSpec);
		given(requestHeadersUriSpec.header(anyString(), anyString())).willAnswer(invocation -> requestHeadersUriSpec);
		given(requestHeadersUriSpec.retrieve()).willReturn(responseSpec);
		given(responseSpec.bodyToMono(SnsUserInfoResponse.class))
			.willReturn(Mono.just(kakaoUserInfoMockResponse));
	}

	private LoginTokenResponse createTokenMockResponse() {
		return LoginTokenResponse.builder()
			.tokenType("Bearer")
			.accessToken("test-access-token")
			.expiresIn(21599)
			.refreshToken("test-refresh-token")
			.refreshTokenExpiresIn(5183999)
			.build();
	}

	private SnsUserInfoResponse createKakaoUserInfoMockResponse() {
		SnsUserInfoResponse.KakaoAccount kakaoAccount = SnsUserInfoResponse.KakaoAccount.builder()
			.email("kakaoLoginTest@example.com")
			.build();

		return SnsUserInfoResponse.builder()
			.id(1212343456L)
			.connectedAt("2025-10-11T09:00:00Z")
			.kakaoAccount(kakaoAccount)
			.build();
	}
}
