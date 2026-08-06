package com.yeolcheong.mub.member.client;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;

/**
 * 스토어 구매 검증 결과(플랫폼 공통). 검증기(Apple/Google)가 스토어 응답을 정규화하여 반환한다.
 */
@Getter
@Builder
public class StoreVerificationResult {

	/** 검증 성공 여부(유효한 구독). */
	private final boolean valid;

	/** 검증된 상품 ID. */
	private final String productId;

	/** 스토어 거래 식별자(Apple originalTransactionId / Google purchaseToken). 멱등 키. */
	private final String storeTransactionId;

	/** 만료(다음 결제) 일시. */
	private final LocalDateTime expiresAt;

	/** 자동 갱신 여부. */
	private final boolean autoRenewing;

	public static StoreVerificationResult invalid() {
		return StoreVerificationResult.builder().valid(false).build();
	}
}
