package com.yeolcheong.mub.member.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * member_coupons 테이블의 used_reference_type 컬럼 정의
 * 쿠폰이 사용된 대상의 유형 (타 서비스 식별자 — FK 아님)
 */
@Getter
@RequiredArgsConstructor
public enum CouponReferenceType {

	GROUP_ACTIVITY("모임 활동"),      // group-service
	SPACE_RESERVATION("공간 예약"),   // space-service
	SUBSCRIPTION("구독 결제");

	private final String description;
}
