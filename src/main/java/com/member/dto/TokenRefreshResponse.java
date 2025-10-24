package com.member.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TokenRefreshResponse {

	private String tokenType;
	private String accessToken;
	private Long expiresIn;
}
