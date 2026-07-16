package com.yeolcheong.mub.member.dto;

import java.util.List;

import com.yeolcheong.mub.member.domain.InterestOption;
import com.yeolcheong.mub.member.domain.InterestType;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfileUpdateRequest {

	@NotBlank(message = "닉네임은 필수입니다")
	@Size(max = 10, message = "닉네임은 최대 10자까지 가능합니다")
	@Pattern(regexp = "^[가-힣a-zA-Z0-9]+$", message = "닉네임은 한글, 영문, 숫자만 가능합니다")
	private String nickname;

	@Size(max = 30, message = "한줄소개는 최대 30자까지 가능합니다")
	private String bio;

	@NotBlank(message = "활동 지역(시/도)은 필수입니다")
	private String distCode1;

	@NotBlank(message = "활동 지역(구/시)은 필수입니다")
	private String distCode2;

	@NotNull(message = "관심사는 필수입니다")
	@Size(min = 1, max = 3, message = "관심사는 1개 이상 3개 이하로 선택해야 합니다")
	@Valid
	private List<InterestRequest> interests;

	@Getter
	@NoArgsConstructor
	@AllArgsConstructor
	public static class InterestRequest {

		@NotNull(message = "관심사 유형은 필수입니다")
		private InterestType interestType;

		// 선호 편의시설(옵션)은 선택 사항 — 미입력(null)·0개 허용
		private List<InterestOption> options;
	}
}
