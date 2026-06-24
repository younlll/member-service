package com.yeolcheong.mub.member.service;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yeolcheong.mub.member.client.KakaoClient;
import com.yeolcheong.mub.member.domain.Member;
import com.yeolcheong.mub.member.domain.MemberStatus;
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
		log.info("Social login started");

		LoginTokenResponse loginTokenResponse = kakaoClient.fetchAccessToken(code);
		SnsUserInfoResponse snsUserInfoResponse = kakaoClient.fetchUserInfo(loginTokenResponse.getAccessToken());

		return issueTokensForKakaoUser(snsUserInfoResponse);
	}

	@Transactional
	public LoginResponse loginWithKakaoToken(String kakaoAccessToken) {
		log.info("Kakao SDK token login started");

		SnsUserInfoResponse snsUserInfoResponse = kakaoClient.fetchUserInfo(kakaoAccessToken);

		return issueTokensForKakaoUser(snsUserInfoResponse);
	}

	@Transactional
	public TokenRefreshResponse refreshAccessToken(String refreshToken) {
		log.info("Access token reissue requested");

		if (!jwtTokenProvider.validateToken(refreshToken)) {
			log.error("Invalid refresh token");
			throw new MemberServiceApiException("유효하지 않은 Refresh Token입니다", ErrorCode.INVALID_TOKEN);
		}

		Long memberId = jwtTokenProvider.getMemberIdFromToken(refreshToken);

		String storedRefreshToken = refreshTokenRepository.findByMemberId(memberId).orElseThrow(() -> {
			log.error("No refresh token stored in Redis: memberId={}", memberId);
			return new MemberServiceApiException("로그인이 필요합니다", ErrorCode.INVALID_TOKEN);
		});

		if (!refreshToken.equals(storedRefreshToken)) {
			log.error("Refresh token mismatch: memberId={}", memberId);
			throw new MemberServiceApiException("유효하지 않은 Refresh Token입니다", ErrorCode.INVALID_TOKEN);
		}

		Member member = memberService.findById(memberId);

		String newAccessToken = jwtTokenProvider.generateAccessToken(member.getId(), member.getSocialId(),
			member.getSnsProvider().name());
		String newRefreshToken = jwtTokenProvider.generateRefreshToken(member.getId());
		saveRefreshToken(member.getId(), newRefreshToken);

		log.info("Access token reissued: memberId={}", memberId);

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
			log.info("New member created: memberId={}, socialId={}", member.getId(), member.getSocialId());
		} else {
			member = existingMember.get();
			if (MemberStatus.DELETED.equals(member.getStatus())) {
				// 탈퇴 회원 재가입: 같은 레코드를 초기화·재활성화하고 신규 회원처럼 온보딩을 진행시킨다.
				memberService.reactivateForResignup(member);
				isNewMember = true;
				log.info("Withdrawn member re-joined: memberId={}, socialId={}", member.getId(), member.getSocialId());
			} else {
				log.info("Existing member login: memberId={}, socialId={}", member.getId(), member.getSocialId());
			}
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
		log.debug("Refresh token saved: memberId={}", memberId);
	}
}
