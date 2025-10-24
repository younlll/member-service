package com.member.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TokenRefreshRequest {

	@NotBlank(message = "Refresh Token은 필수입니다")
	private String refreshToken;
}
