package com.yeolcheong.mub.member.domain;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("MemberCoupon")
class MemberCouponTest {

	private static final Long MEMBER_ID = 1L;

	@Test
	@DisplayName("issue - creates an AVAILABLE coupon with given fields")
	void issueCreatesAvailableCoupon() {
		// given
		LocalDateTime issuedAt = LocalDateTime.now();
		LocalDateTime expiresAt = issuedAt.plusDays(30);

		// when
		MemberCoupon coupon = MemberCoupon.issue("MUB-CODE", buildPolicy(), MEMBER_ID, issuedAt, expiresAt);

		// then
		assertThat(coupon.getStatus()).isEqualTo(CouponStatus.AVAILABLE);
		assertThat(coupon.getCouponCode()).isEqualTo("MUB-CODE");
		assertThat(coupon.getMemberId()).isEqualTo(MEMBER_ID);
		assertThat(coupon.getIssuedAt()).isEqualTo(issuedAt);
		assertThat(coupon.getExpiresAt()).isEqualTo(expiresAt);
		assertThat(coupon.getUsedAt()).isNull();
	}

	@Test
	@DisplayName("isUsable - true when AVAILABLE and before expiry")
	void isUsableTrueWhenAvailableAndBeforeExpiry() {
		// given
		LocalDateTime now = LocalDateTime.now();
		MemberCoupon coupon = MemberCoupon.issue("MUB-CODE", buildPolicy(), MEMBER_ID, now, now.plusDays(30));

		// when & then
		assertThat(coupon.isUsable(now)).isTrue();
	}

	@Test
	@DisplayName("isUsable - false at exact expiry boundary")
	void isUsableFalseAtExpiryBoundary() {
		// given
		LocalDateTime now = LocalDateTime.now();
		LocalDateTime expiresAt = now.plusDays(30);
		MemberCoupon coupon = MemberCoupon.issue("MUB-CODE", buildPolicy(), MEMBER_ID, now, expiresAt);

		// when & then — at == expiresAt 는 사용 불가 (before 조건)
		assertThat(coupon.isUsable(expiresAt)).isFalse();
	}

	@Test
	@DisplayName("markUsed - transitions to USED and records reference")
	void markUsedTransitionsToUsed() {
		// given
		LocalDateTime now = LocalDateTime.now();
		MemberCoupon coupon = MemberCoupon.issue("MUB-CODE", buildPolicy(), MEMBER_ID, now, now.plusDays(30));

		// when
		coupon.markUsed(CouponReferenceType.GROUP_ACTIVITY, 99L, now);

		// then
		assertThat(coupon.getStatus()).isEqualTo(CouponStatus.USED);
		assertThat(coupon.getUsedReferenceType()).isEqualTo(CouponReferenceType.GROUP_ACTIVITY);
		assertThat(coupon.getUsedReferenceId()).isEqualTo(99L);
		assertThat(coupon.getUsedAt()).isEqualTo(now);
		assertThat(coupon.isUsable(now)).isFalse();
	}

	@Test
	@DisplayName("restore - returns USED coupon back to AVAILABLE and clears reference")
	void restoreReturnsToAvailable() {
		// given
		LocalDateTime now = LocalDateTime.now();
		MemberCoupon coupon = MemberCoupon.issue("MUB-CODE", buildPolicy(), MEMBER_ID, now, now.plusDays(30));
		coupon.markUsed(CouponReferenceType.GROUP_ACTIVITY, 99L, now);

		// when
		coupon.restore();

		// then
		assertThat(coupon.getStatus()).isEqualTo(CouponStatus.AVAILABLE);
		assertThat(coupon.getUsedReferenceType()).isNull();
		assertThat(coupon.getUsedReferenceId()).isNull();
		assertThat(coupon.getUsedAt()).isNull();
	}

	@Test
	@DisplayName("expire - transitions to EXPIRED")
	void expireTransitionsToExpired() {
		// given
		LocalDateTime now = LocalDateTime.now();
		MemberCoupon coupon = MemberCoupon.issue("MUB-CODE", buildPolicy(), MEMBER_ID, now, now.plusDays(30));

		// when
		coupon.expire();

		// then
		assertThat(coupon.getStatus()).isEqualTo(CouponStatus.EXPIRED);
		assertThat(coupon.isUsable(now)).isFalse();
	}

	private CouponPolicy buildPolicy() {
		return CouponPolicy.builder()
			.name("모임 3회 무료 체험권")
			.discountType(DiscountType.VOUCHER)
			.applyTarget(ApplyTarget.GROUP)
			.minOrderAmount(0)
			.defaultIssueQuantity(3)
			.validDays(30)
			.isActive(true)
			.build();
	}
}
