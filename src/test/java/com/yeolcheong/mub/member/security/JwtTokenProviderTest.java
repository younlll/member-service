package com.yeolcheong.mub.member.security;

import static org.assertj.core.api.AssertionsForClassTypes.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.yeolcheong.mub.member.domain.SnsProvider;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtTokenProvider unit tests")
class JwtTokenProviderTest {

	private static final String TEST_SECRET =
		"test-secret-key-for-unit-tests-aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
	private static final long ACCESS_TOKEN_EXPIRATION_MS = 86_400_000L; // 1 day
	private static final long REFRESH_TOKEN_EXPIRATION_MS = 1_209_600_000L; // 14 days

	private JwtTokenProvider jwtTokenProvider;

	@BeforeEach
	void setUp() {
		// pure Mockito 환경에서 JwtProperties를 수동으로 구성
		JwtProperties jwtProperties = new JwtProperties();
		jwtProperties.setSecret(TEST_SECRET);
		jwtProperties.setAccessTokenExpiration(ACCESS_TOKEN_EXPIRATION_MS);
		jwtProperties.setRefreshTokenExpiration(REFRESH_TOKEN_EXPIRATION_MS);
		jwtTokenProvider = new JwtTokenProvider(jwtProperties);
	}

	@Test
	@DisplayName("generateAccessToken - returns three-segment JWT")
	void shouldGenerateAccessToken() {
		// given
		Long memberId = 1L;
		String socialId = "1234567890";

		// when
		String token = jwtTokenProvider.generateAccessToken(memberId, socialId, SnsProvider.KAKAO.name());

		// then
		assertThat(token).isNotNull();
		assertThat(token).isNotEmpty();
		assertThat(token).contains(".");
		assertThat(token.split("\\.")).hasSize(3);    // header.payload.signature
	}

	@Test
	@DisplayName("generateRefreshToken - returns three-segment JWT")
	void shouldGenerateRefreshToken() {
		// given
		Long memberId = 1L;

		// when
		String token = jwtTokenProvider.generateRefreshToken(memberId);

		// then
		assertThat(token).isNotNull();
		assertThat(token).isNotEmpty();
		assertThat(token.split("\\.")).hasSize(3);
	}

	@Test
	@DisplayName("validateToken - returns true for token signed with same secret")
	void shouldSuccessToValidateToken() {
		// given
		Long memberId = 1L;
		String socialId = "1234567890";
		String token = jwtTokenProvider.generateAccessToken(memberId, socialId, SnsProvider.KAKAO.name());

		// when
		boolean isValid = jwtTokenProvider.validateToken(token);

		// then
		assertThat(isValid).isTrue();
	}

	@Test
	@DisplayName("validateToken - returns false for malformed token")
	void shouldFailToValidateInvalidToken() {
		// given
		String invalidToken = "invalid.token.signature";

		// when
		boolean isValid = jwtTokenProvider.validateToken(invalidToken);

		// then
		assertThat(isValid).isFalse();
	}

	@Test
	@DisplayName("getMemberIdFromToken - extracts member id from access token subject")
	void shouldExtractMemberIdFromToken() {
		// given
		Long memberId = 1L;
		String socialId = "1234567890";
		String token = jwtTokenProvider.generateAccessToken(memberId, socialId, SnsProvider.KAKAO.name());

		// when
		Long extractedMemberId = jwtTokenProvider.getMemberIdFromToken(token);

		// then
		assertThat(extractedMemberId).isEqualTo(memberId);
	}

	@Test
	@DisplayName("generateAccessToken vs generateRefreshToken - produces different tokens")
	void shouldHaveDifferentExpirationTimes() {
		// given
		Long memberId = 1L;
		String socialId = "1234567890";

		// when
		String accessToken = jwtTokenProvider.generateAccessToken(memberId, socialId, SnsProvider.KAKAO.name());
		String refreshToken = jwtTokenProvider.generateRefreshToken(memberId);

		// then
		assertThat(accessToken).isNotEqualTo(refreshToken);
	}

	@Test
	@DisplayName("getAccessTokenExpiresIn - returns configured expiration in seconds")
	void shouldReturnAccessTokenExpiresInSeconds() {
		// when
		Long expireIn = jwtTokenProvider.getAccessTokenExpiresIn();

		// then
		assertThat(expireIn).isPositive();
		assertThat(expireIn).isEqualTo(ACCESS_TOKEN_EXPIRATION_MS / 1000);
	}
}
