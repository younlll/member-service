package com.yeolcheong.mub.member.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.IntStream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.yeolcheong.mub.member.config.CouponProperties;
import com.yeolcheong.mub.member.domain.CouponPolicy;
import com.yeolcheong.mub.member.domain.MemberCoupon;
import com.yeolcheong.mub.member.exception.ErrorCode;
import com.yeolcheong.mub.member.exception.MemberServiceApiException;
import com.yeolcheong.mub.member.repository.CouponPolicyRepository;
import com.yeolcheong.mub.member.repository.MemberCouponRepository;
import com.yeolcheong.mub.member.util.CouponCodeGenerator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class CouponService {

	private static final String WELCOME_COUPON_PREFIX = "MUB";

	private final CouponPolicyRepository couponPolicyRepository;
	private final MemberCouponRepository memberCouponRepository;
	private final CouponProperties couponProperties;

	/**
	 * 회원가입(온보딩) 완료 시 활동이용권을 발급한다.
	 * 가입 트랜잭션과 분리(REQUIRES_NEW)되어, 발급 실패가 가입을 롤백시키지 않는다.
	 * 정책의 default_issue_quantity 만큼(예: 모임 3회 체험권 = 3매) 발급한다.
	 *
	 * @param memberId 발급 대상 회원 id
	 * @return 발급된 쿠폰 목록
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public List<MemberCoupon> issueWelcomeVoucher(Long memberId) {
		Long policyId = couponProperties.getPolicyId();
		if (policyId == null) {
			throw new MemberServiceApiException(
				"활동이용권 정책이 설정되지 않았습니다", ErrorCode.COUPON_POLICY_NOT_FOUND);
		}

		CouponPolicy policy = couponPolicyRepository.findById(policyId)
			.orElseThrow(() -> new MemberServiceApiException(
				"활동이용권 정책을 찾을 수 없습니다(policyId=" + policyId + ")", ErrorCode.COUPON_POLICY_NOT_FOUND));

		return issueByPolicy(memberId, policy);
	}

	private List<MemberCoupon> issueByPolicy(Long memberId, CouponPolicy policy) {
		int quantity = policy.getDefaultIssueQuantity() != null ? policy.getDefaultIssueQuantity() : 0;
		Integer validDays = policy.getValidDays();

		if (!policy.isIssuable() || quantity < 1 || validDays == null) {
			throw new MemberServiceApiException(
				"활동이용권 정책 설정이 올바르지 않습니다(policyId=" + policy.getId() + ")",
				ErrorCode.COUPON_POLICY_INVALID);
		}

		LocalDateTime issuedAt = LocalDateTime.now();
		LocalDateTime expiresAt = issuedAt.plusDays(validDays);

		List<MemberCoupon> coupons = IntStream.range(0, quantity)
			.mapToObj(i -> MemberCoupon.issue(
				CouponCodeGenerator.generate(WELCOME_COUPON_PREFIX), policy, memberId, issuedAt, expiresAt))
			.toList();

		List<MemberCoupon> saved = memberCouponRepository.saveAll(coupons);
		log.info("Welcome voucher issued: memberId={}, policyId={}, quantity={}",
			memberId, policy.getId(), saved.size());

		return saved;
	}
}
