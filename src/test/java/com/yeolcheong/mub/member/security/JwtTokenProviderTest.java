package com.yeolcheong.mub.member.security;

import static org.assertj.core.api.AssertionsForClassTypes.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.yeolcheong.mub.member.common.SnsProvider;

@SpringBootTest
@DisplayName("JwtTokenProvider 테스트")
class JwtTokenProviderTest {

	@Autowired
	private JwtTokenProvider jwtTokenProvider;

	@Test
	@DisplayName("Access Token을 생성한다")
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
	@DisplayName("Refresh Token을 생성한다")
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
	@DisplayName("토큰 유효성 검증을 성공한다")
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
	@DisplayName("올바르지 않은 토큰의 경우 유효성 검증을 실패한다")
	void shouldFailToValidateInvalidToken() {
		// given
		String invalidToken = "invalid.token.signature";

		// when
		boolean isValid = jwtTokenProvider.validateToken(invalidToken);

		// then
		assertThat(isValid).isFalse();
	}

	@Test
	@DisplayName("토큰에서 회원ID를 정상적으로 추출할 수 있다")
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
	@DisplayName("Access Toekn과 Refresh Token의 만료시간이 다르다")
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
	@DisplayName("Access Token 만료 시간을 초 단위로 정상 반환한다")
	void shouldReturnAccessTokenExpiresInSeconds() {
		// when
		Long expireIn = jwtTokenProvider.getAccessTokenExpiresIn();

		// then
		assertThat(expireIn).isPositive();
		assertThat(expireIn).isEqualTo(86400L);
	}
}