package com.yeolcheong.mub.member.util;

import java.util.UUID;

/**
 * 쿠폰 코드 생성기.
 * "{prefix}-{16자리 대문자 16진수}" 형태의 고유 코드를 생성한다.
 * 최종 유일성은 member_coupons.coupon_code 의 UNIQUE 제약으로 보장한다.
 */
public final class CouponCodeGenerator {

	private static final String DELIMITER = "-";
	private static final int RANDOM_LENGTH = 16;

	private CouponCodeGenerator() {
	}

	public static String generate(String prefix) {
		String random = UUID.randomUUID()
			.toString()
			.replace(DELIMITER, "")
			.substring(0, RANDOM_LENGTH)
			.toUpperCase();
		return prefix + DELIMITER + random;
	}
}
