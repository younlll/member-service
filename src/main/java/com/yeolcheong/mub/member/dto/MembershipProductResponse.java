package com.yeolcheong.mub.member.dto;

import java.time.LocalDate;
import java.util.List;

import com.yeolcheong.mub.member.domain.MembershipCohort;
import com.yeolcheong.mub.member.domain.MembershipPlan;

import lombok.Builder;
import lombok.Getter;

/**
 * 멤버십 상품 조회 응답(멤버십 결제 소개 화면).
 * 상품·기수·모집기간·혜택·현재 가입자 수와, 앱이 구매를 요청할 스토어별 상품 ID를 담는다.
 */
@Getter
@Builder
public class MembershipProductResponse {

	private String planName;
	private int monthlyPrice;
	private List<String> benefits;

	private int cohortNumber;
	private LocalDate recruitStartDate;
	private LocalDate recruitEndDate;
	private boolean recruiting;

	private long currentSubscribers;

	private String iosProductId;
	private String androidProductId;

	public static MembershipProductResponse from(
		MembershipPlan plan, MembershipCohort cohort, boolean recruiting, long currentSubscribers) {
		return MembershipProductResponse.builder()
			.planName(plan.getName())
			.monthlyPrice(plan.getMonthlyPrice())
			.benefits(plan.getBenefits())
			.cohortNumber(cohort.getCohortNumber())
			.recruitStartDate(cohort.getRecruitStartDate())
			.recruitEndDate(cohort.getRecruitEndDate())
			.recruiting(recruiting)
			.currentSubscribers(currentSubscribers)
			.iosProductId(plan.getIosProductId())
			.androidProductId(plan.getAndroidProductId())
			.build();
	}
}
