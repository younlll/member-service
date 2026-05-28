package com.yeolcheong.mub.member.service;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yeolcheong.mub.member.client.KakaoClient;
import com.yeolcheong.mub.member.domain.Member;
import com.yeolcheong.mub.member.domain.RefreshToken;
import com.yeolcheong.mub.member.domain.SnsProvider;
import com.yeolcheong.mub.member.dto.LoginResponse;
import com.yeolcheong.mub.member.dto.LoginTokenResponse;
import com.yeolcheong.mub.member.dto.SnsUserInfoResponse;
import com.yeolcheong.mub.member.dto.TokenRefreshResponse;
import com.yeolcheong.mub.member.exception.ErrorCode;
import com.yeolcheong.mub.member.exception.MemberServiceApiException;
import com.yeolcheong.mub.member.repository.RefreshTokenRepository;
import com.yeolcheong.mub.member.security.JwtTokenProvider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class AuthService {

	private final KakaoClient kakaoClient;
	private final JwtTokenProvider jwtTokenProvider;
	private final MemberService memberService;
	private final RefreshTokenRepository refreshTokenRepository;

	public String getAuthUrl() {
		return kakaoClient.buildAuthorizationUrl();
	}

	@Transactional
	public LoginResponse login(String code) {
		log.info("소셜 로그인 시작");

		LoginTokenResponse loginTokenResponse = kakaoClient.fetchAccessToken(code);
		SnsUserInfoResponse snsUserInfoResponse = kakaoClient.fetchUserInfo(loginTokenResponse.getAccessToken());

		return issueTokensForKakaoUser(snsUserInfoResponse);
	}

	@Transactional
	public LoginResponse loginWithKakaoToken(String kakaoAccessToken) {
		log.info("카카오 SDK 토큰 로그인 시작");

		SnsUserInfoResponse snsUserInfoResponse = kakaoClient.fetchUserInfo(kakaoAccessToken);

		return issueTokensForKakaoUser(snsUserInfoResponse);
	}

	@Transactional
	public TokenRefreshResponse refreshAccessToken(String refreshToken) {
		log.info("Access Token 재발급 요청");

		if (!jwtTokenProvider.validateToken(refreshToken)) {
			log.error("유효하지 않은 Refresh Token");
			throw new MemberServiceApiException("유효하지 않은 Refresh Token입니다", ErrorCode.INVALID_TOKEN);
		}

		Long memberId = jwtTokenProvider.getMemberIdFromToken(refreshToken);

		String storedRefreshToken = refreshTokenRepository.findByMemberId(memberId).orElseThrow(() -> {
			log.error("Redis에 저장된 Refresh Token이 없음: memberId={}", memberId);
			return new MemberServiceApiException("로그인이 필요합니다", ErrorCode.INVALID_TOKEN);
		});

		if (!refreshToken.equals(storedRefreshToken)) {
			log.error("Refresh Token 불일치: memberId={}", memberId);
			throw new MemberServiceApiException("유효하지 않은 Refresh Token입니다", ErrorCode.INVALID_TOKEN);
		}

		Member member = memberService.findById(memberId);

		String newAccessToken = jwtTokenProvider.generateAccessToken(member.getId(), member.getSocialId(),
			member.getSnsProvider().name());
		String newRefreshToken = jwtTokenProvider.generateRefreshToken(member.getId());
		saveRefreshToken(member.getId(), newRefreshToken);

		log.info("Access Token 재발급 성공: memberId={}", memberId);

		return TokenRefreshResponse.builder()
			.memberId(String.valueOf(member.getId()))
			.kakaoId(member.getSocialId())
			.email(member.getEmail())
			.tokenType("Bearer")
			.accessToken(newAccessToken)
			.expiresIn(jwtTokenProvider.getAccessTokenExpiresIn())
			.refreshToken(newRefreshToken)
			.refreshTokenExpiresIn(jwtTokenProvider.getRefreshTokenExpiresIn())
			.connectedAt(null)
			.isNewMember(false)
			.build();
	}

	private LoginResponse issueTokensForKakaoUser(SnsUserInfoResponse snsUserInfoResponse) {
		Optional<Member> existingMember = memberService.findBySocialId(
			SnsProvider.KAKAO, snsUserInfoResponse.getKakaoIdAsString());

		boolean isNewMember = existingMember.isEmpty();
		Member member;
		if (isNewMember) {
			member = memberService.createdFromSnsUser(snsUserInfoResponse);
			log.info("신규 회원 생성: memberId={}, socialId={}", member.getId(), member.getSocialId());
		} else {
			member = existingMember.get();
			log.info("기존 회원 로그인: memberId={}, socialId={}", member.getId(), member.getSocialId());
		}

		String accessToken = jwtTokenProvider.generateAccessToken(member.getId(), member.getSocialId(),
			member.getSnsProvider().name());
		String refreshToken = jwtTokenProvider.generateRefreshToken(member.getId());

		saveRefreshToken(member.getId(), refreshToken);

		return LoginResponse.of(accessToken, refreshToken, jwtTokenProvider.getAccessTokenExpiresIn(),
			snsUserInfoResponse, isNewMember, String.valueOf(member.getId()));
	}

	private void saveRefreshToken(Long memberId, String token) {
		long ttlSeconds = jwtTokenProvider.getRefreshTokenExpiresIn() / 1000;

		RefreshToken refreshToken = RefreshToken.builder()
			.memberId(memberId)
			.token(token)
			.expiresAt(LocalDateTime.now().plusSeconds(ttlSeconds))
			.build();

		refreshTokenRepository.save(refreshToken, ttlSeconds);
		log.debug("Refresh Token 저장: memberId={}", memberId);
	}
}
