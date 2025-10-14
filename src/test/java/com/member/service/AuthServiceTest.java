package com.member.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.web.reactive.function.client.WebClient;

import com.member.common.KakaoProperties;
import com.member.common.MemberStatus;
import com.member.common.SnsProvider;
import com.member.domain.Member;
import com.member.dto.LoginResponse;
import com.member.dto.LoginTokenResponse;
import com.member.dto.SnsUserInfoResponse;

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

	@InjectMocks
	private AuthService authService;

	private static final Long TEST_MEMBER_ID = 1L;
	private static final String TEST_KAKAO_ID = "1212343456";
	private static final String TEST_KAKAO_EMAIL = "kakaoLoginTest@example.com";

	@BeforeEach
	void setUp() {
		given(kakaoProperties.getAuthUrl()).willReturn("https://kauth.kakao.com/oauth/authorize/");
		given(kakaoProperties.getClientId()).willReturn("test-client-id");
		given(kakaoProperties.getRedirectUri()).willReturn("http://localhost:8081/api/auth/kakao/callback");
		given(kakaoProperties.getTokenUrl()).willReturn("https://kauth.kakao.com/oauth/token");
		given(kakaoProperties.getUserInfoUrl()).willReturn("https://kapi.kakao.com/v2/user/me");
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
	@DisplayName("신규 회원 로그인 시, 소셜로그인 정보를 바탕으로 회원을 생성한다")
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

		setupWebClientMocks(loginTokenMockResponse, kakaoUserInfoMockResponse);

		LoginResponse response = authService.login(authorizationCode);

		assertThat(response).isNotNull();
		assertThat(response.getTokenType()).isEqualTo("Bearer");
		assertThat(response.getAccessToken()).isEqualTo("test-access-token");
		assertThat(response.getExpiresIn()).isEqualTo(21599);
		assertThat(response.getRefreshToken()).isEqualTo("test-refresh-token");
		assertThat(response.getRefreshTokenExpiresIn()).isEqualTo(5183999);

		assertThat(response.getKakaoId()).isEqualTo(TEST_KAKAO_ID);
		assertThat(response.getEmail()).isEqualTo(TEST_KAKAO_EMAIL);
		assertThat(response.getConnectedAt()).isNotNull();

		assertThat(response.getIsNewMember()).isTrue();

		verify(memberService).createdFromSnsUser(any(SnsUserInfoResponse.class));
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
