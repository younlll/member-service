package com.member.security;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtTokenProvider {

	private final JwtProperties jwtProperties;

	private SecretKey getSigningKey() {
		byte[] keyBytes = jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8);
		return Keys.hmacShaKeyFor(keyBytes);
	}

	public String generateAccessToken(Long memberId, String socialId, String snsProvider) {
		Date now = new Date();
		Date expiryDate = new Date(now.getTime() + jwtProperties.getAccessTokenExpiration());

		return Jwts.builder()
			.subject(String.valueOf(memberId))
			.claim("socialId", socialId)
			.claim("snsProvider", snsProvider)
			.claim("type", "ACCESS")
			.issuedAt(now)
			.expiration(expiryDate)
			.signWith(getSigningKey())
			.compact();
	}

	public String generateRefreshToken(Long memberId) {
		Date now = new Date();
		Date expiryDate = new Date(now.getTime() + jwtProperties.getRefreshTokenExpiration());

		return Jwts.builder()
			.subject(String.valueOf(memberId))
			.claim("type", "REFRESH")
			.issuedAt(now)
			.expiration(expiryDate)
			.signWith(getSigningKey())
			.compact();
	}

	public boolean validateToken(String token) {
		try {
			Jwts.parser()
				.verifyWith(getSigningKey())
				.build()
				.parseSignedClaims(token);
			return true;
		} catch (SecurityException e) {
			log.error("Invalid JWT signature: {}", e.getMessage());
		} catch (MalformedJwtException e) {
			log.error("Invalid JWT token: {}", e.getMessage());
		} catch (ExpiredJwtException e) {
			log.error("Expired JWT token: {}", e.getMessage());
		} catch (UnsupportedJwtException e) {
			log.error("Unsupported JWT token: {}", e.getMessage());
		} catch (IllegalArgumentException e) {
			log.error("JWT claims string is empty: {}", e.getMessage());
		}
		return false;
	}

	public Long getMemberIdFromToken(String token) {
		Claims claims = parseClaims(token);
		return Long.parseLong(claims.getSubject());
	}

	public Long getAccessTokenExpiresIn() {
		return jwtProperties.getAccessTokenExpiration() / 1000;
	}

	public Long getRefreshTokenExpiresIn() {
		return jwtProperties.getRefreshTokenExpiration();
	}

	private Claims parseClaims(String token) {
		try {
			return Jwts.parser()
				.verifyWith(getSigningKey())
				.build()
				.parseSignedClaims(token)
				.getPayload();
		} catch (ExpiredJwtException e) {
			return e.getClaims();
		}
	}
}
