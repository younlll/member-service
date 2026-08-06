package com.yeolcheong.mub.member.dto;

import com.yeolcheong.mub.member.domain.MembershipPlatform;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 멤버십 구매 검증 요청. 앱이 스토어 결제 완료 후 검증 정보를 서버로 전달한다.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MembershipPurchaseRequest {

	@NotNull(message = "결제 플랫폼은 필수입니다")
	private MembershipPlatform platform;

	@NotBlank(message = "상품 ID는 필수입니다")
	private String productId;

	@NotBlank(message = "구매 토큰은 필수입니다")
	private String purchaseToken;
}
