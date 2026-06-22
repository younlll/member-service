package com.yeolcheong.mub.member.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.yeolcheong.mub.member.config.CouponProperties;
import com.yeolcheong.mub.member.domain.ApplyTarget;
import com.yeolcheong.mub.member.domain.CouponPolicy;
import com.yeolcheong.mub.member.domain.CouponStatus;
import com.yeolcheong.mub.member.domain.DiscountType;
import com.yeolcheong.mub.member.domain.MemberCoupon;
import com.yeolcheong.mub.member.exception.ErrorCode;
import com.yeolcheong.mub.member.exception.MemberServiceApiException;
import com.yeolcheong.mub.member.repository.CouponPolicyRepository;
import com.yeolcheong.mub.member.repository.MemberCouponRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("CouponService")
class CouponServiceTest {

	private static final Long WELCOME_POLICY_ID = 1L;
	private static final Long MEMBER_ID = 100L;

	@Mock
	private CouponPolicyRepository couponPolicyRepository;
	@Mock
	private MemberCouponRepository memberCouponRepository;
	@Mock
	private CouponProperties couponProperties;

	@InjectMocks
	private CouponService couponService;

	// =========================================================
	// issueWelcomeVoucher — 성공 케이스
	// =========================================================
	@Nested
	@DisplayName("issueWelcomeVoucher - success")
	class IssueWelcomeVoucherSuccess {

		@Test
		@DisplayName("should issue 3 available coupons for a 3-use voucher policy")
		void shouldIssueThreeAvailableCoupons() {
			// given
			CouponPolicy policy = buildVoucherPolicy(3, 30);
			given(couponProperties.getPolicyId()).willReturn(WELCOME_POLICY_ID);
			given(couponPolicyRepository.findById(WELCOME_POLICY_ID)).willReturn(Optional.of(policy));
			given(memberCouponRepository.saveAll(anyList())).willAnswer(inv -> inv.getArgument(0));

			// when
			List<MemberCoupon> result = couponService.issueWelcomeVoucher(MEMBER_ID);

			// then
			List<MemberCoupon> saved = captureSavedCoupons();
			assertThat(result).hasSize(3);
			assertThat(saved).hasSize(3).allSatisfy(coupon -> {
				assertThat(coupon.getStatus()).isEqualTo(CouponStatus.AVAILABLE);
				assertThat(coupon.getMemberId()).isEqualTo(MEMBER_ID);
				assertThat(coupon.getPolicy()).isSameAs(policy);
				assertThat(coupon.getCouponCode()).startsWith("MUB-");
				assertThat(coupon.getExpiresAt()).isAfter(coupon.getIssuedAt());
			});
		}

		@Test
		@DisplayName("should issue coupons with unique codes")
		void shouldIssueCouponsWithUniqueCodes() {
			// given
			given(couponProperties.getPolicyId()).willReturn(WELCOME_POLICY_ID);
			given(couponPolicyRepository.findById(WELCOME_POLICY_ID))
				.willReturn(Optional.of(buildVoucherPolicy(3, 30)));
			given(memberCouponRepository.saveAll(anyList())).willAnswer(inv -> inv.getArgument(0));

			// when
			couponService.issueWelcomeVoucher(MEMBER_ID);

			// then
			List<MemberCoupon> saved = captureSavedCoupons();
			long distinctCodes = saved.stream().map(MemberCoupon::getCouponCode).distinct().count();
			assertThat(distinctCodes).isEqualTo(saved.size());
		}

		@ParameterizedTest(name = "should issue {0} coupon(s) according to default_issue_quantity")
		@DisplayName("should issue exactly default_issue_quantity coupons")
		@ValueSource(ints = {1, 3, 5})
		void shouldIssueExactlyDefaultQuantityCoupons(int quantity) {
			// given
			given(couponProperties.getPolicyId()).willReturn(WELCOME_POLICY_ID);
			given(couponPolicyRepository.findById(WELCOME_POLICY_ID))
				.willReturn(Optional.of(buildVoucherPolicy(quantity, 30)));
			given(memberCouponRepository.saveAll(anyList())).willAnswer(inv -> inv.getArgument(0));

			// when
			List<MemberCoupon> result = couponService.issueWelcomeVoucher(MEMBER_ID);

			// then
			assertThat(result).hasSize(quantity);
		}
	}

	// =========================================================
	// issueWelcomeVoucher — 정책 조회 실패
	// =========================================================
	@Nested
	@DisplayName("issueWelcomeVoucher - policy lookup failure")
	class PolicyLookupFailure {

		@Test
		@DisplayName("should throw COUPON_POLICY_NOT_FOUND when policy id is not configured")
		void shouldThrowWhenPolicyIdNotConfigured() {
			// given
			given(couponProperties.getPolicyId()).willReturn(null);

			// when & then
			assertThatThrownBy(() -> couponService.issueWelcomeVoucher(MEMBER_ID))
				.isInstanceOf(MemberServiceApiException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.COUPON_POLICY_NOT_FOUND);

			then(couponPolicyRepository).should(never()).findById(any());
			then(memberCouponRepository).should(never()).saveAll(anyList());
		}

		@Test
		@DisplayName("should throw COUPON_POLICY_NOT_FOUND when policy does not exist")
		void shouldThrowWhenPolicyDoesNotExist() {
			// given
			given(couponProperties.getPolicyId()).willReturn(WELCOME_POLICY_ID);
			given(couponPolicyRepository.findById(WELCOME_POLICY_ID)).willReturn(Optional.empty());

			// when & then
			assertThatThrownBy(() -> couponService.issueWelcomeVoucher(MEMBER_ID))
				.isInstanceOf(MemberServiceApiException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.COUPON_POLICY_NOT_FOUND);

			then(memberCouponRepository).should(never()).saveAll(anyList());
		}
	}

	// =========================================================
	// issueWelcomeVoucher — 정책 설정 오류
	// =========================================================
	@Nested
	@DisplayName("issueWelcomeVoucher - invalid policy configuration")
	class InvalidPolicyConfiguration {

		@Test
		@DisplayName("should throw COUPON_POLICY_INVALID when valid_days is null")
		void shouldThrowWhenValidDaysIsNull() {
			// given
			givenPolicy(buildVoucherPolicy(3, null));

			// when & then
			assertInvalidPolicy();
		}

		@Test
		@DisplayName("should throw COUPON_POLICY_INVALID when issue quantity is zero")
		void shouldThrowWhenQuantityIsZero() {
			// given
			givenPolicy(buildVoucherPolicy(0, 30));

			// when & then
			assertInvalidPolicy();
		}

		@Test
		@DisplayName("should throw COUPON_POLICY_INVALID when policy is inactive")
		void shouldThrowWhenPolicyInactive() {
			// given
			CouponPolicy inactive = CouponPolicy.builder()
				.name("비활성 정책")
				.discountType(DiscountType.VOUCHER)
				.applyTarget(ApplyTarget.GROUP)
				.minOrderAmount(0)
				.defaultIssueQuantity(3)
				.validDays(30)
				.isActive(false)
				.build();
			givenPolicy(inactive);

			// when & then
			assertInvalidPolicy();
		}

		private void givenPolicy(CouponPolicy policy) {
			given(couponProperties.getPolicyId()).willReturn(WELCOME_POLICY_ID);
			given(couponPolicyRepository.findById(WELCOME_POLICY_ID)).willReturn(Optional.of(policy));
		}

		private void assertInvalidPolicy() {
			assertThatThrownBy(() -> couponService.issueWelcomeVoucher(MEMBER_ID))
				.isInstanceOf(MemberServiceApiException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.COUPON_POLICY_INVALID);

			then(memberCouponRepository).should(never()).saveAll(anyList());
		}
	}

	// =========================================================
	// Helper
	// =========================================================
	private CouponPolicy buildVoucherPolicy(int quantity, Integer validDays) {
		return CouponPolicy.builder()
			.name("모임 3회 무료 체험권")
			.discountType(DiscountType.VOUCHER)
			.applyTarget(ApplyTarget.GROUP)
			.minOrderAmount(0)
			.defaultIssueQuantity(quantity)
			.validDays(validDays)
			.isActive(true)
			.build();
	}

	@SuppressWarnings("unchecked")
	private List<MemberCoupon> captureSavedCoupons() {
		ArgumentCaptor<List<MemberCoupon>> captor = ArgumentCaptor.forClass(List.class);
		then(memberCouponRepository).should().saveAll(captor.capture());
		return captor.getValue();
	}
}
