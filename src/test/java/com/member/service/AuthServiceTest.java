package com.member.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;

import com.member.dto.LoginTokenResponse;

import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService 단위 테스트")
class AuthServiceTest {

	@Mock
	private WebClient webClient;

	@Mock
	private WebClient.RequestBodyUriSpec requestBodyUriSpec;

	@Mock
	private WebClient.RequestBodySpec requestBodySpec;

	@Mock
	private WebClient.RequestHeadersSpec<?> requestHeadersSpec;

	@Mock
	private WebClient.ResponseSpec responseSpec;

	@InjectMocks
	private AuthService authService;

	@BeforeEach
	void setUp() {
		ReflectionTestUtils.setField(authService, "authUrl", "https://kauth.kakao.com/oauth/authorize/");
		ReflectionTestUtils.setField(authService, "clientId", "test-client-id");
		ReflectionTestUtils.setField(authService, "redirectUri", "http://localhost:8081/api/auth/kakao/callback");
		ReflectionTestUtils.setField(authService, "tokenUrl", "https://kauth.kakao.com/oauth/token");
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
	@DisplayName("유효한 인가 코드로 토큰 발급에 성공한다")
	void shouldGetTokenSuccessfully() {
		// given
		String authorizationCode = "test-authorization-code";

		LoginTokenResponse mockResponse = new LoginTokenResponse();
		mockResponse.setTokenType("bearer");
		mockResponse.setAccessToken("test-access-token");
		mockResponse.setExpiresIn(21599);
		mockResponse.setRefreshToken("test-refresh-token");
		mockResponse.setRefreshTokenExpiresIn(5183999);

		given(webClient.post()).willReturn(requestBodyUriSpec);
		given(requestBodyUriSpec.uri(anyString())).willReturn(requestBodySpec);
		given(requestBodySpec.contentType(any())).willReturn(requestBodySpec);
		given(requestBodySpec.bodyValue(any())).willAnswer(invocation -> requestHeadersSpec);
		given(requestHeadersSpec.retrieve()).willReturn(responseSpec);
		given(responseSpec.bodyToMono(LoginTokenResponse.class))
			.willReturn(Mono.just(mockResponse));

		LoginTokenResponse response = authService.login(authorizationCode);

		assertThat(response).isNotNull();
		assertThat(response.getTokenType()).isEqualTo("bearer");
		assertThat(response.getAccessToken()).isEqualTo("test-access-token");
		assertThat(response.getExpiresIn()).isEqualTo(21599);
		assertThat(response.getRefreshToken()).isEqualTo("test-refresh-token");
		assertThat(response.getRefreshTokenExpiresIn()).isEqualTo(5183999);
	}
}
