package com.member.dto;

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
	private Integer expiresIn;
	private String refreshToken;
	private Integer refreshTokenExpiresIn;

	// SnsUserInfoResponse 로그인 계정의 사용자 정보
	private String kakaoId;
	private String connectedAt;
	private String email;

	public static LoginResponse of(LoginTokenResponse loginTokenResponse, SnsUserInfoResponse snsUserInfoResponse) {
		return LoginResponse.builder()
			.tokenType(loginTokenResponse.getTokenType())
			.accessToken(loginTokenResponse.getAccessToken())
			.expiresIn(loginTokenResponse.getExpiresIn())
			.refreshToken(loginTokenResponse.getRefreshToken())
			.refreshTokenExpiresIn(loginTokenResponse.getRefreshTokenExpiresIn())
			.kakaoId(snsUserInfoResponse.getKakaoIdAsString())
			.connectedAt(snsUserInfoResponse.getConnectedAt())
			.email(snsUserInfoResponse.getKakaoAccount().getEmail())
			.build();
	}
}
