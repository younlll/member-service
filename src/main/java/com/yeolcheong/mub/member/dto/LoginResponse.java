package com.yeolcheong.mub.member.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class LoginResponse {

	// LoginTokenResponse 토큰 정보
	private String tokenType;
	private String accessToken;
	private Long expiresIn;
	private String refreshToken;
	private Long refreshTokenExpiresIn;

	// SnsUserInfoResponse 로그인 계정의 사용자 정보
	private String kakaoId;
	private String connectedAt;
	private String email;

	private Boolean isNewMember;
	private String memberId;

	public static LoginResponse of(
		String accessToken,
		String refreshToken,
		Long expiresIn,
		SnsUserInfoResponse snsUserInfoResponse,
		Boolean isNewMember,
		String memberId
	) {
		return LoginResponse.builder()
			.tokenType("Bearer")
			.accessToken(accessToken)
			.expiresIn(expiresIn)
			.refreshToken(refreshToken)
			.kakaoId(snsUserInfoResponse.getKakaoIdAsString())
			.connectedAt(snsUserInfoResponse.getConnectedAt())
			.email(snsUserInfoResponse.getKakaoAccount().getEmail())
			.isNewMember(isNewMember)
			.memberId(memberId)
			.build();
	}
}
