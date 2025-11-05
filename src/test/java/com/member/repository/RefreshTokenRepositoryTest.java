package com.member.repository;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

import com.member.domain.RefreshToken;

@SpringBootTest
@DisplayName("RefreshTokenRepository 테스트")
class RefreshTokenRepositoryTest {

	@Autowired
	private RefreshTokenRepository refreshTokenRepository;

	@Autowired
	private RedisTemplate<String, String> redisTemplate;

	@AfterEach
	void tearDown() {
		Objects.requireNonNull(redisTemplate.getConnectionFactory()).getConnection().serverCommands().flushDb();
	}

	@Test
	@DisplayName("Refresh Token 저장 및 조회")
	void shouldSaveAndFindRefreshToken() {
		// given
		Long memberId = 1L;
		String token = "test-refresh-token";
		RefreshToken refreshToken = RefreshToken.builder()
			.memberId(memberId)
			.token(token)
			.expiresAt(LocalDateTime.now().plusDays(7))
			.build();

		// when
		refreshTokenRepository.save(refreshToken, 604800L);

		// then
		Optional<String> foundToken = refreshTokenRepository.findByMemberId(memberId);
		assertThat(foundToken).isPresent();
		assertThat(foundToken.get()).isEqualTo(token);
	}

	@Test
	@DisplayName("Token으로 회원 ID 조회 (역방향)")
	void shouldFindMemberIdByToken() {
		// given
		Long memberId = 2L;
		String token = "test-token-12345";
		RefreshToken refreshToken = RefreshToken.builder()
			.memberId(memberId)
			.token(token)
			.expiresAt(LocalDateTime.now().plusDays(7))
			.build();

		refreshTokenRepository.save(refreshToken, 604800L);

		String memberKey = "refresh_token:member:" + memberId;
		String tokenKey = "refresh_token:token:" + token;
		String storedToken = redisTemplate.opsForValue().get(memberKey);
		String storedMemberId = redisTemplate.opsForValue().get(tokenKey);
		System.out.println("정방향 Key: " + memberKey + " → Value: " + storedToken);
		System.out.println("역방향 Key: " + tokenKey + " → Value: " + storedMemberId);

		// when
		Optional<Long> foundMemberId = refreshTokenRepository.findMemberIdByToken(token);

		System.out.println("조회 결과: " + foundMemberId);
		// then
		assertThat(foundMemberId).isPresent();
		assertThat(foundMemberId.get()).isEqualTo(memberId);
		System.out.println("✅ 역방향 조회 성공: token=" + token + " → memberId=" + foundMemberId.get());
	}

	@Test
	@DisplayName("Refresh Token 삭제")
	void shouldDeleteRefreshToken() {
		// given
		Long memberId = 3L;
		String token = "token-to-delete";
		RefreshToken refreshToken = RefreshToken.builder()
			.memberId(memberId)
			.token(token)
			.expiresAt(LocalDateTime.now().plusDays(7))
			.build();

		refreshTokenRepository.save(refreshToken, 604800L);

		// when
		refreshTokenRepository.deleteByMemberId(memberId);

		// then
		Optional<String> foundToken = refreshTokenRepository.findByMemberId(memberId);
		assertThat(foundToken).isEmpty();

		Optional<Long> foundMemberId = refreshTokenRepository.findMemberIdByToken(token);
		assertThat(foundMemberId).isEmpty();
	}

	@Test
	@DisplayName("존재하지 않는 Token 조회")
	void shouldReturnEmptyForNonExistentToken() {
		// when
		Optional<Long> foundMemberId = refreshTokenRepository.findMemberIdByToken("non-existent-token");

		// then
		assertThat(foundMemberId).isEmpty();
	}
}
