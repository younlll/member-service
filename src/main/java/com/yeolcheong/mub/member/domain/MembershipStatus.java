package com.yeolcheong.mub.member.domain;

/**
 * 회원별 멤버십(구독) 상태.
 */
public enum MembershipStatus {
	// 이용 중(유효한 구독)
	ACTIVE,
	// 만료(갱신되지 않아 기간 종료)
	EXPIRED,
	// 해지(사용자가 자동갱신 중단)
	CANCELLED
}
