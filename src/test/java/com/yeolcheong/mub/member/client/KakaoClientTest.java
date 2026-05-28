package com.yeolcheong.mub.member.client;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.SoftAssertions.*;
import static org.mockito.BDDMockito.*;

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
import com.yeolcheong.mub.member.dto.LoginTokenResponse;
import com.yeolcheong.mub.member.dto.SnsUserInfoResponse;
import com.yeolcheong.mub.member.exception.ErrorCode;
import com.yeolcheong.mub.member.exception.MemberServiceApiException;

import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("KakaoClient")
class KakaoClientTest {

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

	@InjectMocks
	private KakaoClient kakaoClient;

	@BeforeEach
	void setUp() {
		given(kakaoProperties.getAuthUrl()).willReturn("https://kauth.kakao.com/oauth/authorize");
		given(kakaoProperties.getClientId()).willReturn("test-client-id");
		given(kakaoProperties.getRedirectUri()).willReturn("http://localhost:8083/api/auth/kakao/callback");
		given(kakaoProperties.getTokenUrl()).willReturn("https://kauth.kakao.com/oauth/token");
		given(kakaoProperties.getUserInfoUrl()).willReturn("https://kapi.kakao.com/v2/user/me");
	}

	// =========================================================
	// buildAuthorizationUrl
	// =========================================================
	@Nested
	@DisplayName("buildAuthorizationUrl")
	class BuildAuthorizationUrl {

		@Test
		@DisplayName("should return url containing all required oauth parameters")
		void shouldReturnUrlContainingAllRequiredOauthParameters() {
			// when
			String authUrl = kakaoClient.buildAuthorizationUrl();

			// then
			assertSoftly(softly -> {
				softly.assertThat(authUrl).startsWith("https://kauth.kakao.com/oauth/authorize");
				softly.assertThat(authUrl).contains("client_id=test-client-id");
				softly.assertThat(authUrl).contains("redirect_uri=http://localhost:8083/api/auth/kakao/callback");
				softly.assertThat(authUrl).contains("response_type=code");
			});
		}

		@Test
		@DisplayName("should reflect dynamic clientId in auth url")
		void shouldReflectDynamicClientIdInAuthUrl() {
			// given
			given(kakaoProperties.getClientId()).willReturn("another-client-id");

			// when
			String authUrl = kakaoClient.buildAuthorizationUrl();

			// then
			assertThat(authUrl).contains("client_id=another-client-id");
		}
	}

	// =========================================================
	// fetchAccessToken
	// =========================================================
	@Nested
	@DisplayName("fetchAccessToken")
	class FetchAccessToken {

		@Test
		@DisplayName("should return token response when kakao token api succeeds")
		void shouldReturnTokenResponseWhenKakaoTokenApiSucceeds() {
			// given
			LoginTokenResponse expected = buildTokenResponse();
			setupTokenWebClientMock(Mono.just(expected));

			// when
			LoginTokenResponse result = kakaoClient.fetchAccessToken("auth-code");

			// then
			assertThat(result).isEqualTo(expected);
		}

		@ParameterizedTest(name = "should throw INVALID_TOKEN_VALUE when kakao token api returns 400 for code=\"{0}\"")
		@ValueSource(strings = {"expired-code", "already-used-code", ""})
		@DisplayName("should throw INVALID_TOKEN_VALUE when kakao token api returns 400")
		void shouldThrowInvalidTokenValueWhenKakaoTokenApiReturns400(String invalidCode) {
			// given
			setupTokenWebClientMock(Mono.error(makeWebClientException(HttpStatus.BAD_REQUEST)));

			// when & then
			assertThatThrownBy(() -> kakaoClient.fetchAccessToken(invalidCode))
				.isInstanceOf(MemberServiceApiException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.INVALID_TOKEN_VALUE);
		}

		@Test
		@DisplayName("should throw INVALID_TOKEN when kakao token api returns 401")
		void shouldThrowInvalidTokenWhenKakaoTokenApiReturns401() {
			// given
			setupTokenWebClientMock(Mono.error(makeWebClientException(HttpStatus.UNAUTHORIZED)));

			// when & then
			assertThatThrownBy(() -> kakaoClient.fetchAccessToken("any-code"))
				.isInstanceOf(MemberServiceApiException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.INVALID_TOKEN);
		}

		@Test
		@DisplayName("should throw EXTERNAL_API_ERROR when kakao token api returns 500")
		void shouldThrowExternalApiErrorWhenKakaoTokenApiReturns500() {
			// given
			setupTokenWebClientMock(Mono.error(makeWebClientException(HttpStatus.INTERNAL_SERVER_ERROR)));

			// when & then
			assertThatThrownBy(() -> kakaoClient.fetchAccessToken("any-code"))
				.isInstanceOf(MemberServiceApiException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.EXTERNAL_API_ERROR);
		}

		@Test
		@DisplayName("should throw EXTERNAL_API_ERROR when kakao token response is null")
		void shouldThrowExternalApiErrorWhenKakaoTokenResponseIsNull() {
			// given
			setupTokenWebClientMock(Mono.justOrEmpty(null));

			// when & then
			assertThatThrownBy(() -> kakaoClient.fetchAccessToken("any-code"))
				.isInstanceOf(MemberServiceApiException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.EXTERNAL_API_ERROR);
		}

		@Test
		@DisplayName("should throw EXTERNAL_API_ERROR when unexpected exception occurs during token request")
		void shouldThrowExternalApiErrorWhenUnexpectedExceptionOccursDuringTokenRequest() {
			// given
			setupTokenWebClientMock(Mono.error(new RuntimeException("Network failure")));

			// when & then
			assertThatThrownBy(() -> kakaoClient.fetchAccessToken("any-code"))
				.isInstanceOf(MemberServiceApiException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.EXTERNAL_API_ERROR);
		}
	}

	// =========================================================
	// fetchUserInfo
	// =========================================================
	@Nested
	@DisplayName("fetchUserInfo")
	class FetchUserInfo {

		@Test
		@DisplayName("should return user info when kakao user-info api succeeds")
		void shouldReturnUserInfoWhenKakaoUserInfoApiSucceeds() {
			// given
			SnsUserInfoResponse expected = buildUserInfo();
			setupUserInfoWebClientMock(Mono.just(expected));

			// when
			SnsUserInfoResponse result = kakaoClient.fetchUserInfo("access-token");

			// then
			assertThat(result).isEqualTo(expected);
		}

		@Test
		@DisplayName("should throw INVALID_TOKEN when kakao user-info api returns 401")
		void shouldThrowInvalidTokenWhenKakaoUserInfoApiReturns401() {
			// given
			setupUserInfoWebClientMock(Mono.error(makeWebClientException(HttpStatus.UNAUTHORIZED)));

			// when & then
			assertThatThrownBy(() -> kakaoClient.fetchUserInfo("any-token"))
				.isInstanceOf(MemberServiceApiException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.INVALID_TOKEN);
		}

		@Test
		@DisplayName("should throw EXTERNAL_API_ERROR when kakao user-info api returns 500")
		void shouldThrowExternalApiErrorWhenKakaoUserInfoApiReturns500() {
			// given
			setupUserInfoWebClientMock(Mono.error(makeWebClientException(HttpStatus.INTERNAL_SERVER_ERROR)));

			// when & then
			assertThatThrownBy(() -> kakaoClient.fetchUserInfo("any-token"))
				.isInstanceOf(MemberServiceApiException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.EXTERNAL_API_ERROR);
		}

		@Test
		@DisplayName("should throw EXTERNAL_API_ERROR when kakao user-info response is null")
		void shouldThrowExternalApiErrorWhenKakaoUserInfoResponseIsNull() {
			// given
			setupUserInfoWebClientMock(Mono.justOrEmpty(null));

			// when & then
			assertThatThrownBy(() -> kakaoClient.fetchUserInfo("any-token"))
				.isInstanceOf(MemberServiceApiException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.EXTERNAL_API_ERROR);
		}

		@Test
		@DisplayName("should throw EXTERNAL_API_ERROR when unexpected exception occurs during user-info request")
		void shouldThrowExternalApiErrorWhenUnexpectedExceptionOccursDuringUserInfoRequest() {
			// given
			setupUserInfoWebClientMock(Mono.error(new RuntimeException("Timeout")));

			// when & then
			assertThatThrownBy(() -> kakaoClient.fetchUserInfo("any-token"))
				.isInstanceOf(MemberServiceApiException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.EXTERNAL_API_ERROR);
		}
	}

	// =========================================================
	// Helpers
	// =========================================================

	private LoginTokenResponse buildTokenResponse() {
		return LoginTokenResponse.builder()
			.tokenType("Bearer")
			.accessToken("kakao-access-token")
			.expiresIn(21599)
			.refreshToken("kakao-refresh-token")
			.refreshTokenExpiresIn(5183999)
			.build();
	}

	private SnsUserInfoResponse buildUserInfo() {
		return SnsUserInfoResponse.builder()
			.id(1212343456L)
			.connectedAt("2025-10-11T09:00:00Z")
			.kakaoAccount(SnsUserInfoResponse.KakaoAccount.builder().email("test@example.com").build())
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
}
