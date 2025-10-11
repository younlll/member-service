package com.member.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KakaoUserInfoResponse {

	@JsonProperty("id")
	private Long id;

	@JsonProperty("connected_at")
	private String connectedAt;

	@JsonProperty("kakao_account")
	private KakaoAccount kakaoAccount;

	@Getter
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class KakaoAccount {
		@JsonProperty("email")
		private String email;
	}

	public String getKakaoIdAsString() {
		return String.valueOf(id);
	}

	public String getEmail() {
		if (kakaoAccount != null) {
			return kakaoAccount.getEmail();
		}
		return null;
	}
}
