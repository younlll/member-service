package com.yeolcheong.mub.member.repository;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.SoftAssertions.*;
import static org.mockito.BDDMockito.*;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import com.yeolcheong.mub.member.domain.RefreshToken;

@ExtendWith(MockitoExtension.class)
@DisplayName("RefreshTokenRepository")
class RefreshTokenRepositoryTest {

	private static final Long MEMBER_ID = 1L;
	private static final String TOKEN = "jwt-refresh-token";
	private static final long TTL_SECONDS = 1209600L; // 14일
	private static final String MEMBER_KEY = "refresh_token:member:1";
	private static final String TOKEN_KEY = "refresh_token:token:jwt-refresh-token";
	@Mock
	private RedisTemplate<String, String> redisTemplate;
	@Mock
	private ValueOperations<String, String> valueOperations;
	@InjectMocks
	private RefreshTokenRepository refreshTokenRepository;


	// =========================================================
	// Helper
	// =========================================================
	private RefreshToken buildRefreshToken(Long memberId, String token) {
		return RefreshToken.builder()
			.memberId(memberId)
			.token(token)
			.build();
	}

	// =========================================================
	// save
	// =========================================================
	@Nested
	@DisplayName("save")
	class Save {

		@BeforeEach
		void setUp() {
			given(redisTemplate.opsForValue()).willReturn(valueOperations);
		}

		@Test
		@DisplayName("should store member-to-token and token-to-member entries with correct keys")
		void shouldStoreBothEntriesWithCorrectKeys() {
			// given
			RefreshToken refreshToken = buildRefreshToken(MEMBER_ID, TOKEN);
			ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
			ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
			ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.forClass(Duration.class);

			// when
			refreshTokenRepository.save(refreshToken, TTL_SECONDS);

			// then — set 2번 호출 (member key, token key)
			verify(valueOperations, times(2))
				.set(keyCaptor.capture(), valueCaptor.capture(), ttlCaptor.capture());

			List<String> keys = keyCaptor.getAllValues();
			List<String> values = valueCaptor.getAllValues();

			assertSoftly(softly -> {
				softly.assertThat(keys).containsExactlyInAnyOrder(MEMBER_KEY, TOKEN_KEY);
				softly.assertThat(values).containsExactlyInAnyOrder(TOKEN, String.valueOf(MEMBER_ID));
			});
		}

		@Test
		@DisplayName("should apply correct TTL when saving refresh token")
		void shouldApplyCorrectTtlWhenSavingRefreshToken() {
			// given
			RefreshToken refreshToken = buildRefreshToken(MEMBER_ID, TOKEN);
			ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.forClass(Duration.class);

			// when
			refreshTokenRepository.save(refreshToken, TTL_SECONDS);

			// then
			verify(valueOperations, times(2)).set(anyString(), anyString(), ttlCaptor.capture());
			ttlCaptor.getAllValues().forEach(ttl ->
				assertThat(ttl).isEqualTo(Duration.ofSeconds(TTL_SECONDS))
			);
		}

		@Test
		@DisplayName("should overwrite existing token when same memberId is saved again")
		void shouldOverwriteExistingTokenWhenSameMemberIdIsSavedAgain() {
			// given
			RefreshToken first = buildRefreshToken(MEMBER_ID, "old-token");
			RefreshToken second = buildRefreshToken(MEMBER_ID, "new-token");

			// when — 두 번 저장
			refreshTokenRepository.save(first, TTL_SECONDS);
			refreshTokenRepository.save(second, TTL_SECONDS);

			// then — set 총 4번 (각 저장마다 2번)
			verify(valueOperations, times(4)).set(anyString(), anyString(), any(Duration.class));
		}
	}

	// =========================================================
	// findByMemberId
	// =========================================================
	@Nested
	@DisplayName("findByMemberId")
	class FindByMemberId {

		@BeforeEach
		void setUp() {
			given(redisTemplate.opsForValue()).willReturn(valueOperations);
		}

		@Test
		@DisplayName("should return token when member key exists in redis")
		void shouldReturnTokenWhenMemberKeyExistsInRedis() {
			// given
			given(valueOperations.get(MEMBER_KEY)).willReturn(TOKEN);

			// when
			Optional<String> result = refreshTokenRepository.findByMemberId(MEMBER_ID);

			// then
			assertSoftly(softly -> {
				softly.assertThat(result).isPresent();
				softly.assertThat(result.get()).isEqualTo(TOKEN);
			});
			verify(valueOperations).get(MEMBER_KEY);
		}

		@Test
		@DisplayName("should return empty Optional when member key does not exist in redis")
		void shouldReturnEmptyOptionalWhenMemberKeyDoesNotExistInRedis() {
			// given
			given(valueOperations.get(MEMBER_KEY)).willReturn(null);

			// when
			Optional<String> result = refreshTokenRepository.findByMemberId(MEMBER_ID);

			// then
			assertThat(result).isEmpty();
		}

		@ParameterizedTest(name = "should query correct member key for memberId={0}")
		@DisplayName("should query with correct redis key format for various memberIds")
		@ValueSource(longs = {1L, 100L, 999999L, Long.MAX_VALUE})
		void shouldQueryWithCorrectRedisKeyForVariousMemberIds(long memberId) {
			// given
			String expectedKey = "refresh_token:member:" + memberId;
			given(valueOperations.get(expectedKey)).willReturn(TOKEN);

			// when
			refreshTokenRepository.findByMemberId(memberId);

			// then
			verify(valueOperations).get(expectedKey);
		}
	}

	// =========================================================
	// findMemberIdByToken
	// =========================================================
	@Nested
	@DisplayName("findMemberIdByToken")
	class FindMemberIdByToken {

		@BeforeEach
		void setUp() {
			given(redisTemplate.opsForValue()).willReturn(valueOperations);
		}

		@Test
		@DisplayName("should return memberId when token key exists in redis")
		void shouldReturnMemberIdWhenTokenKeyExistsInRedis() {
			// given
			given(valueOperations.get(TOKEN_KEY)).willReturn(String.valueOf(MEMBER_ID));

			// when
			Optional<Long> result = refreshTokenRepository.findMemberIdByToken(TOKEN);

			// then
			assertSoftly(softly -> {
				softly.assertThat(result).isPresent();
				softly.assertThat(result.get()).isEqualTo(MEMBER_ID);
			});
		}

		@Test
		@DisplayName("should return empty Optional when token key does not exist in redis")
		void shouldReturnEmptyOptionalWhenTokenKeyDoesNotExistInRedis() {
			// given
			given(valueOperations.get(TOKEN_KEY)).willReturn(null);

			// when
			Optional<Long> result = refreshTokenRepository.findMemberIdByToken(TOKEN);

			// then
			assertThat(result).isEmpty();
		}

		@Test
		@DisplayName("should return empty Optional when stored memberId value is not a valid number")
		void shouldReturnEmptyOptionalWhenStoredMemberIdValueIsNotValidNumber() {
			// given — Redis에 숫자가 아닌 값이 저장된 경우 (데이터 오염)
			given(valueOperations.get(TOKEN_KEY)).willReturn("not-a-number");

			// when
			Optional<Long> result = refreshTokenRepository.findMemberIdByToken(TOKEN);

			// then
			assertThat(result).isEmpty();
		}

		@ParameterizedTest(name = "should return empty Optional for blank or null token \"{0}\"")
		@DisplayName("should return empty Optional for blank or null tokens")
		@NullAndEmptySource
		@ValueSource(strings = {"   "})
		void shouldReturnEmptyOptionalForBlankOrNullTokens(String blankToken) {
			// given
			String blankTokenKey = "refresh_token:token:" + blankToken;
			given(valueOperations.get(blankTokenKey)).willReturn(null);

			// when
			Optional<Long> result = refreshTokenRepository.findMemberIdByToken(blankToken);

			// then
			assertThat(result).isEmpty();
		}
	}

	// =========================================================
	// deleteByMemberId
	// =========================================================
	@Nested
	@DisplayName("deleteByMemberId")
	class DeleteByMemberId {

		@BeforeEach
		void setUp() {
			given(redisTemplate.opsForValue()).willReturn(valueOperations);
		}

		@Test
		@DisplayName("should delete both member key and token key when token exists")
		void shouldDeleteBothMemberKeyAndTokenKeyWhenTokenExists() {
			// given — 토큰이 존재하는 경우
			given(valueOperations.get(MEMBER_KEY)).willReturn(TOKEN);

			// when
			refreshTokenRepository.deleteByMemberId(MEMBER_ID);

			// then
			verify(redisTemplate).delete(MEMBER_KEY);
			verify(redisTemplate).delete(TOKEN_KEY);
		}

		@Test
		@DisplayName("should delete only member key when token does not exist in redis")
		void shouldDeleteOnlyMemberKeyWhenTokenDoesNotExistInRedis() {
			// given — 토큰이 이미 만료/삭제된 경우
			given(valueOperations.get(MEMBER_KEY)).willReturn(null);

			// when
			refreshTokenRepository.deleteByMemberId(MEMBER_ID);

			// then — member key만 삭제, token key는 delete 미호출
			verify(redisTemplate).delete(MEMBER_KEY);
			verify(redisTemplate, never()).delete(TOKEN_KEY);
		}

		@Test
		@DisplayName("should delete member key before looking up token key")
		void shouldLookupTokenBeforeDeletingMemberKey() {
			// given
			given(valueOperations.get(MEMBER_KEY)).willReturn(TOKEN);

			// when
			refreshTokenRepository.deleteByMemberId(MEMBER_ID);

			// then — findByMemberId(get) → delete(memberKey) → delete(tokenKey) 순서
			var inOrder = inOrder(valueOperations, redisTemplate);
			inOrder.verify(valueOperations).get(MEMBER_KEY);
			inOrder.verify(redisTemplate).delete(MEMBER_KEY);
			inOrder.verify(redisTemplate).delete(TOKEN_KEY);
		}
	}

	// =========================================================
	// existsByMemberId
	// =========================================================
	@Nested
	@DisplayName("existsByMemberId")
	class ExistsByMemberId {

		@Test
		@DisplayName("should return true when member key exists in redis")
		void shouldReturnTrueWhenMemberKeyExistsInRedis() {
			// given
			given(redisTemplate.hasKey(MEMBER_KEY)).willReturn(true);

			// when
			boolean result = refreshTokenRepository.existsByMemberId(MEMBER_ID);

			// then
			assertThat(result).isTrue();
			verify(redisTemplate).hasKey(MEMBER_KEY);
		}

		@Test
		@DisplayName("should return false when member key does not exist in redis")
		void shouldReturnFalseWhenMemberKeyDoesNotExistInRedis() {
			// given
			given(redisTemplate.hasKey(MEMBER_KEY)).willReturn(false);

			// when
			boolean result = refreshTokenRepository.existsByMemberId(MEMBER_ID);

			// then
			assertThat(result).isFalse();
		}

		@Test
		@DisplayName("should throw NullPointerException when redis hasKey returns null (documents current unsafe behavior)")
		void shouldThrowNpeWhenRedisHasKeyReturnsNull() {
			// given — RedisTemplate.hasKey()는 null을 반환할 수 있음
			given(redisTemplate.hasKey(MEMBER_KEY)).willReturn(null);

			// when & then — 현재 구현은 null 방어가 없어 NPE 발생
			// TODO: Repository에 null 방어 로직 추가 후 false 반환으로 변경해야 함
			assertThatThrownBy(() -> refreshTokenRepository.existsByMemberId(MEMBER_ID))
				.isInstanceOf(NullPointerException.class);
		}

		@ParameterizedTest(name = "should check correct redis key for memberId={0}")
		@DisplayName("should check correct redis key format for various memberIds")
		@ValueSource(longs = {1L, 42L, 999L})
		void shouldCheckCorrectRedisKeyForVariousMemberIds(long memberId) {
			// given
			String expectedKey = "refresh_token:member:" + memberId;
			given(redisTemplate.hasKey(expectedKey)).willReturn(true);

			// when
			boolean result = refreshTokenRepository.existsByMemberId(memberId);

			// then
			assertThat(result).isTrue();
			verify(redisTemplate).hasKey(expectedKey);
		}
	}
}
