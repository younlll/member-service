package com.yeolcheong.mub.member.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OnboardingResponse {

	private Long memberId;
	private String nickname;
	private String message;

	public static OnboardingResponse of(Long memberId, String nickname) {
		return OnboardingResponse.builder()
			.memberId(memberId)
			.nickname(nickname)
			.message("회원가입이 완료되었습니다")
			.build();
	}
}
