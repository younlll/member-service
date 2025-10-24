package com.member.repository;

import java.time.Duration;
import java.util.Optional;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import com.member.domain.RefreshToken;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Repository
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenRepository {

	private final RedisTemplate<String, String> redisTemplate;

	private static final String MEMBER_TO_TOKEN_PREFIX = "refresh_token:member:";
	private static final String TOKEN_TO_MEMBER_PREFIX = "refresh_token:token:";

	public void save(RefreshToken refreshToken, long ttlSeconds) {
		Long memberId = refreshToken.getMemberId();
		String token = refreshToken.getToken();

		Duration ttl = Duration.ofSeconds(ttlSeconds);

		String memberKey = generateMemberKey(memberId);
		redisTemplate.opsForValue().set(memberKey, token, ttl);

		String tokenKey = generateTokenKey(token);
		redisTemplate.opsForValue().set(tokenKey, String.valueOf(memberId), ttl);

		log.debug("Refresh Token 저장: memberId={}, ttl={}초", memberId, ttlSeconds);
	}

	public Optional<String> findByMemberId(Long memberId) {
		String key = generateMemberKey(memberId);
		String token = redisTemplate.opsForValue().get(key);

		log.debug("Refresh Token 조회: memberid={}, found={}", memberId, token != null);
		return Optional.ofNullable(token);
	}

	public Optional<Long> findMemberIdByToken(String token) {
		String key = generateTokenKey(token);
		String memberIdStr = redisTemplate.opsForValue().get(key);

		if (memberIdStr == null) {
			log.debug("Token으로 회원 ID를 찾지 못함");
			return Optional.empty();
		}

		try {
			Long memberId = Long.parseLong(memberIdStr);
			log.debug("Token으로 회원 ID 찾음: memberId={}", memberId);
			return Optional.of(memberId);
		} catch (NumberFormatException e) {
			log.error("회원 ID 파싱 실패: memberIdStr={}", memberIdStr, e);
			return Optional.empty();
		}
	}

	public void deleteByMemberId(Long memberId) {
		Optional<String> tokenOpt = findByMemberId(memberId);

		String memberKey = generateMemberKey(memberId);
		redisTemplate.delete(memberKey);

		tokenOpt.ifPresent(token -> {
			String tokenKey = generateTokenKey(token);
			redisTemplate.delete(tokenKey);
		});
		log.debug("Refresh Token 삭제: memberId={}", memberId);
	}

	public boolean existsByMemberId(Long memberId) {
		String key = generateMemberKey(memberId);
		return redisTemplate.hasKey(key);
	}

	private String generateMemberKey(Long memberId) {
		return MEMBER_TO_TOKEN_PREFIX + memberId;
	}

	private String generateTokenKey(String token) {
		return TOKEN_TO_MEMBER_PREFIX + token;
	}
}
