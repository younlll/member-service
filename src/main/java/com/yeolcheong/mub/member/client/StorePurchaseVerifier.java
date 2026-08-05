package com.yeolcheong.mub.member.client;

import com.yeolcheong.mub.member.domain.MembershipPlatform;

/**
 * 스토어 구매 검증 추상화. 플랫폼별 구현(Apple/Google)을 서비스가 플랫폼으로 선택한다.
 */
public interface StorePurchaseVerifier {

	/** 이 검증기가 담당하는 플랫폼. */
	MembershipPlatform platform();

	/**
	 * 스토어에 구매를 검증하고 정규화된 결과를 반환한다.
	 *
	 * @param productId    클라이언트가 구매한 상품 ID
	 * @param purchaseToken 구매 토큰/거래 식별자(Apple transactionId / Google purchaseToken)
	 * @return 검증 결과(유효 시 만료일·거래ID 포함)
	 */
	StoreVerificationResult verify(String productId, String purchaseToken);
}
