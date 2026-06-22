package com.yeolcheong.mub.member.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * member_coupons 테이블의 status 컬럼 정의
 * 쿠폰 상태
 */
@Getter
@RequiredArgsConstructor
public enum CouponStatus {

	AVAILABLE("사용가능"),
	USED("사용됨"),
	EXPIRED("만료");

	private final String description;
}
