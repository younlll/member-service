package com.yeolcheong.mub.member.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * coupon_policies 테이블의 apply_target 컬럼 정의
 * 쿠폰 적용 대상
 */
@Getter
@RequiredArgsConstructor
public enum ApplyTarget {

	GROUP("모임"),          // 모임 정모 참여
	SPACE("공간"),          // 제휴 공간(카페) 예약 결제
	SUBSCRIPTION("구독");   // 머브 크루 멤버십 구독료

	private final String description;
}
