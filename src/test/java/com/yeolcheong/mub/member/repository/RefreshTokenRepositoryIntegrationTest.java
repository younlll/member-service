package com.yeolcheong.mub.member.repository;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.redis.DataRedisTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.yeolcheong.mub.member.domain.RefreshToken;

@DataRedisTest
@Testcontainers
@Import(RefreshTokenRepository.class)
@DisplayName("RefreshTokenRepository Integration Test")
class RefreshTokenRepositoryIntegrationTest {

	@Container
	static final GenericContainer<?> REDIS =
		new GenericContainer<>(DockerImageName.parse("redis:7.4"))
			.withExposedPorts(6379);

	@DynamicPropertySource
	static void redisProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.data.redis.host", REDIS::getHost);
		registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
	}

	@Autowired private RefreshTokenRepository refreshTokenRepository;
	@Autowired private RedisTemplate<String, String> redisTemplate;

	private static final Long   MEMBER_ID   = 1L;
	private static final String TOKEN       = "real-refresh-token";
	private static final long   TTL_SECONDS = 60L;

	@BeforeEach
	void setUp() {
		// 각 테스트 전 Redis 전체 초기화
		redisTemplate.getConnectionFactory().getConnection()
			.serverCommands()
			.flushAll();
	}

	@Nested
	@DisplayName("save & findByMemberId")
	class SaveAndFind {

		@Test
		@DisplayName("should persist and retrieve token by memberId")
		void shouldPersistAndRetrieveTokenByMemberId() {
			// given
			refreshTokenRepository.save(buildRefreshToken(MEMBER_ID, TOKEN), TTL_SECONDS);

			// when
			Optional<String> result = refreshTokenRepository.findByMemberId(MEMBER_ID);

			// then
			assertSoftly(softly -> {
				softly.assertThat(result).isPresent();
				softly.assertThat(result.get()).isEqualTo(TOKEN);
			});
		}

		@Test
		@DisplayName("should persist and retrieve memberId by token")
		void shouldPersistAndRetrieveMemberIdByToken() {
			// given
			refreshTokenRepository.save(buildRefreshToken(MEMBER_ID, TOKEN), TTL_SECONDS);

			// when
			Optional<Long> result = refreshTokenRepository.findMemberIdByToken(TOKEN);

			// then
			assertSoftly(softly -> {
				softly.assertThat(result).isPresent();
				softly.assertThat(result.get()).isEqualTo(MEMBER_ID);
			});
		}

		@Test
		@DisplayName("should overwrite token when same memberId saved twice")
		void shouldOverwriteTokenWhenSameMemberIdSavedTwice() {
			// given
			refreshTokenRepository.save(buildRefreshToken(MEMBER_ID, "old-token"), TTL_SECONDS);
			refreshTokenRepository.save(buildRefreshToken(MEMBER_ID, "new-token"), TTL_SECONDS);

			// when
			Optional<String> result = refreshTokenRepository.findByMemberId(MEMBER_ID);

			// then
			assertThat(result.get()).isEqualTo("new-token");
		}
	}

	@Nested
	@DisplayName("deleteByMemberId")
	class DeleteByMemberId {

		@Test
		@DisplayName("should remove both keys after delete")
		void shouldRemoveBothKeysAfterDelete() {
			// given
			refreshTokenRepository.save(buildRefreshToken(MEMBER_ID, TOKEN), TTL_SECONDS);

			// when
			refreshTokenRepository.deleteByMemberId(MEMBER_ID);

			// then
			assertSoftly(softly -> {
				softly.assertThat(refreshTokenRepository.findByMemberId(MEMBER_ID)).isEmpty();
				softly.assertThat(refreshTokenRepository.findMemberIdByToken(TOKEN)).isEmpty();
			});
		}

		@Test
		@DisplayName("should not throw when deleting non-existent memberId")
		void shouldNotThrowWhenDeletingNonExistentMemberId() {
			assertThatNoException()
				.isThrownBy(() -> refreshTokenRepository.deleteByMemberId(999L));
		}
	}

	@Nested
	@DisplayName("existsByMemberId")
	class ExistsByMemberId {

		@Test
		@DisplayName("should return true after saving token")
		void shouldReturnTrueAfterSavingToken() {
			refreshTokenRepository.save(buildRefreshToken(MEMBER_ID, TOKEN), TTL_SECONDS);
			assertThat(refreshTokenRepository.existsByMemberId(MEMBER_ID)).isTrue();
		}

		@Test
		@DisplayName("should return false after deleting token")
		void shouldReturnFalseAfterDeletingToken() {
			refreshTokenRepository.save(buildRefreshToken(MEMBER_ID, TOKEN), TTL_SECONDS);
			refreshTokenRepository.deleteByMemberId(MEMBER_ID);
			assertThat(refreshTokenRepository.existsByMemberId(MEMBER_ID)).isFalse();
		}

		@Test
		@DisplayName("should return false for never-stored memberId")
		void shouldReturnFalseForNeverStoredMemberId() {
			assertThat(refreshTokenRepository.existsByMemberId(999L)).isFalse();
		}
	}

	private RefreshToken buildRefreshToken(Long memberId, String token) {
		return RefreshToken.builder()
			.memberId(memberId)
			.token(token)
			.build();
	}
}
