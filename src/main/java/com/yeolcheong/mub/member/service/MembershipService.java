package com.yeolcheong.mub.member.service;

import java.time.LocalDate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yeolcheong.mub.member.domain.Membership;
import com.yeolcheong.mub.member.domain.MembershipCohort;
import com.yeolcheong.mub.member.domain.MembershipPlan;
import com.yeolcheong.mub.member.domain.MembershipStatus;
import com.yeolcheong.mub.member.dto.MembershipProductResponse;
import com.yeolcheong.mub.member.dto.MembershipResponse;
import com.yeolcheong.mub.member.exception.ErrorCode;
import com.yeolcheong.mub.member.exception.MemberServiceApiException;
import com.yeolcheong.mub.member.repository.MembershipCohortRepository;
import com.yeolcheong.mub.member.repository.MembershipPlanRepository;
import com.yeolcheong.mub.member.repository.MembershipRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 멤버십(구독) 상품 조회 서비스. 상품 카탈로그는 DB(플랜·기수)로 관리하며, 스토어 키는 관여하지 않는다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class MembershipService {

	private final MembershipPlanRepository planRepository;
	private final MembershipCohortRepository cohortRepository;
	private final MembershipRepository membershipRepository;

	/**
	 * 현재 판매 중인 멤버십 상품(활성 플랜 + 최신 기수 + 현재 가입자 수)을 조회한다.
	 * 모집 여부는 오늘 날짜가 해당 기수의 모집 기간 안에 있는지로 판정한다.
	 *
	 * @return 멤버십 상품 정보
	 * @throws MemberServiceApiException 활성 상품/기수가 없을 때 {@link ErrorCode#MEMBERSHIP_PRODUCT_NOT_FOUND}
	 */
	public MembershipProductResponse getActiveMembershipProduct() {
		MembershipPlan plan = planRepository.findFirstByActiveTrue()
			.orElseThrow(() -> new MemberServiceApiException(ErrorCode.MEMBERSHIP_PRODUCT_NOT_FOUND));
		MembershipCohort cohort = cohortRepository.findFirstByPlanOrderByCohortNumberDesc(plan)
			.orElseThrow(() -> new MemberServiceApiException(ErrorCode.MEMBERSHIP_PRODUCT_NOT_FOUND));

		boolean recruiting = cohort.isRecruiting(LocalDate.now());
		long currentSubscribers = membershipRepository.countByStatus(MembershipStatus.ACTIVE);

		log.info("Membership product fetched | plan={}, cohort={}, recruiting={}, subscribers={}",
			plan.getName(), cohort.getCohortNumber(), recruiting, currentSubscribers);
		return MembershipProductResponse.from(plan, cohort, recruiting, currentSubscribers);
	}

	/**
	 * 내 멤버십 조회('내 멤버십' 화면). 현재 이용 중(ACTIVE)인 멤버십을 반환한다.
	 *
	 * @param memberId 요청 회원 ID
	 * @return 내 멤버십 정보
	 * @throws MemberServiceApiException 이용 중인 멤버십이 없을 때 {@link ErrorCode#MEMBERSHIP_NOT_FOUND}
	 */
	public MembershipResponse getMyMembership(Long memberId) {
		Membership membership = membershipRepository
			.findFirstByMemberIdAndStatusOrderByExpiresAtDesc(memberId, MembershipStatus.ACTIVE)
			.orElseThrow(() -> new MemberServiceApiException(ErrorCode.MEMBERSHIP_NOT_FOUND));

		log.info("My membership fetched | memberId={}, cohort={}", memberId, membership.getCohort().getCohortNumber());
		return MembershipResponse.from(membership);
	}
}
