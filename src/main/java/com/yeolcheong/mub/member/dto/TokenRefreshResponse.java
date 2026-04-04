package com.yeolcheong.mub.member.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TokenRefreshResponse {

	private String memberId;
	private String kakaoId;
	private String email;
	private String tokenType;
	private String accessToken;
	private Long expiresIn;
	private String refreshToken;
	private Long refreshTokenExpiresIn;
	private String connectedAt;
	private boolean isNewMember;
}
