package com.yeolcheong.mub.member.dto;

import java.util.List;
import java.util.Map;

import com.yeolcheong.mub.member.domain.InterestOption;
import com.yeolcheong.mub.member.domain.InterestType;
import com.yeolcheong.mub.member.domain.TermsType;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
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
public class OnboardingRequest {

	@NotNull(message = "약관 동의 정보는 필수입니다")
	@Valid
	private TermsAgreementRequest termsAgreementRequest;

	@NotBlank(message = "닉네임은 필수입니다")
	@Size(max = 10, message = "닉네임은 최대 10자까지 가능합니다")
	@Pattern(regexp = "^[가-힣a-zA-Z0-9]+$", message = "닉네임은 한글, 영문, 숫자만 가능합니다")
	private String nickname;

	@NotBlank(message = "활동 지역(시/도)은 필수입니다")
	private String distCode1;

	@NotBlank(message = "활동 지역(구/시)은 필수입니다")
	private String distCode2;

	@NotNull(message = "관심사는 필수입니다")
	@Size(min = 1, max = 3, message = "관심사는 1개 이상 3개 이하로 선택해야 합니다")
	@Valid
	private List<InterestRequest> interests;

	// 프로필 이미지 경로(선택). 사전 업로드로 받은 스토리지 중립 상대 경로(예: profile/uuid.png).
	// null/blank 이면 기본 이미지를 사용한다.
	@Size(max = 500, message = "프로필 이미지 경로가 너무 깁니다")
	private String profileImagePath;

	/**
	 * 약관 동의 DTO
	 */
	@Getter
	@NoArgsConstructor
	@AllArgsConstructor
	public static class TermsAgreementRequest {

		@NotNull(message = "서비스 이용약관 동의는 필수입니다")
		@AssertTrue(message = "서비스 이용약관에 동의해야 합니다")
		private Boolean termsOfService;

		@NotNull(message = "개인정보 처리방침 동의는 필수입니다")
		@AssertTrue(message = "개인정보 처리방침에 동의해야 합니다")
		private Boolean privacyPolicy;

		@NotNull(message = "위치기반 서비스 이용약관 동의는 필수입니다")
		@AssertTrue(message = "위치기반 서비스 이용약관에 동의해야 합니다")
		private Boolean locationService;

		private Boolean marketing;

		public Map<TermsType, Boolean> toMap() {
			return Map.of(
				TermsType.TERMS_OF_SERVICE, termsOfService,
				TermsType.PRIVACY_POLICY, privacyPolicy,
				TermsType.LOCATION_SERVICE, locationService,
				TermsType.MARKETING, marketing != null ? marketing : false
			);
		}
	}

	@Getter
	@NoArgsConstructor
	@AllArgsConstructor
	public static class InterestRequest {

		@NotNull(message = "관심사 유형은 필수입니다")
		private InterestType interestType;

		@NotNull(message = "관심사 옵션은 필수입니다")
		@Size(min = 1, message = "관심사별 옵션은 최소 1개 이상 선택해야 합니다")
		private List<InterestOption> options;
	}
}
