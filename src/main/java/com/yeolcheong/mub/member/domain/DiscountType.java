package com.yeolcheong.mub.member.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * coupon_policies 테이블의 discount_type 컬럼 정의
 * 쿠폰 할인 유형
 */
@Getter
@RequiredArgsConstructor
public enum DiscountType {

	VOUCHER("이용권"),   // 비용이 아닌 이용 권한 (혜택값 없음)
	FIXED("정액"),       // 고정 금액 차감
	PERCENT("정률");     // 비율 차감 (최대 한도 설정 가능)

	private final String description;
}
