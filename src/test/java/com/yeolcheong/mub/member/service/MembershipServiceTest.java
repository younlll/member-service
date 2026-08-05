package com.yeolcheong.mub.member.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.yeolcheong.mub.member.domain.MembershipCohort;
import com.yeolcheong.mub.member.domain.MembershipPlan;
import com.yeolcheong.mub.member.domain.MembershipStatus;
import com.yeolcheong.mub.member.dto.MembershipProductResponse;
import com.yeolcheong.mub.member.exception.ErrorCode;
import com.yeolcheong.mub.member.exception.MemberServiceApiException;
import com.yeolcheong.mub.member.repository.MembershipCohortRepository;
import com.yeolcheong.mub.member.repository.MembershipPlanRepository;
import com.yeolcheong.mub.member.repository.MembershipRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("MembershipService")
class MembershipServiceTest {

	@Mock
	private MembershipPlanRepository planRepository;

	@Mock
	private MembershipCohortRepository cohortRepository;

	@Mock
	private MembershipRepository membershipRepository;

	@InjectMocks
	private MembershipService membershipService;

	@Nested
	@DisplayName("getActiveMembershipProduct")
	class GetActiveMembershipProduct {

		@Test
		@DisplayName("returns product with recruiting=true when today is within the recruit window")
		void returnsRecruitingProduct() {
			// given
			MembershipPlan plan = buildPlan();
			MembershipCohort cohort = buildCohort(plan, LocalDate.now().minusDays(1), LocalDate.now().plusDays(10));
			given(planRepository.findFirstByActiveTrue()).willReturn(Optional.of(plan));
			given(cohortRepository.findFirstByPlanOrderByCohortNumberDesc(plan)).willReturn(Optional.of(cohort));
			given(membershipRepository.countByStatus(MembershipStatus.ACTIVE)).willReturn(42L);

			// when
			MembershipProductResponse response = membershipService.getActiveMembershipProduct();

			// then
			assertThat(response.getPlanName()).isEqualTo("머브크루");
			assertThat(response.getMonthlyPrice()).isEqualTo(5000);
			assertThat(response.getCohortNumber()).isEqualTo(3);
			assertThat(response.isRecruiting()).isTrue();
			assertThat(response.getCurrentSubscribers()).isEqualTo(42L);
			assertThat(response.getIosProductId()).isEqualTo("com.mub.app.membership.monthly");
			assertThat(response.getAndroidProductId()).isEqualTo("mub_membership_monthly");
			assertThat(response.getBenefits()).contains("공간 예약 할인");
		}

		@Test
		@DisplayName("returns product with recruiting=false when the recruit window has passed")
		void returnsClosedProduct() {
			// given
			MembershipPlan plan = buildPlan();
			MembershipCohort cohort = buildCohort(plan, LocalDate.now().minusDays(30), LocalDate.now().minusDays(1));
			given(planRepository.findFirstByActiveTrue()).willReturn(Optional.of(plan));
			given(cohortRepository.findFirstByPlanOrderByCohortNumberDesc(plan)).willReturn(Optional.of(cohort));
			given(membershipRepository.countByStatus(MembershipStatus.ACTIVE)).willReturn(0L);

			// when
			MembershipProductResponse response = membershipService.getActiveMembershipProduct();

			// then
			assertThat(response.isRecruiting()).isFalse();
			assertThat(response.getCurrentSubscribers()).isZero();
		}

		@Test
		@DisplayName("throws MEMBERSHIP_PRODUCT_NOT_FOUND when no active plan exists")
		void throwsWhenNoActivePlan() {
			// given
			given(planRepository.findFirstByActiveTrue()).willReturn(Optional.empty());

			// when & then
			assertThatThrownBy(() -> membershipService.getActiveMembershipProduct())
				.isInstanceOf(MemberServiceApiException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.MEMBERSHIP_PRODUCT_NOT_FOUND);
		}

		@Test
		@DisplayName("throws MEMBERSHIP_PRODUCT_NOT_FOUND when the plan has no cohort")
		void throwsWhenNoCohort() {
			// given
			MembershipPlan plan = buildPlan();
			given(planRepository.findFirstByActiveTrue()).willReturn(Optional.of(plan));
			given(cohortRepository.findFirstByPlanOrderByCohortNumberDesc(plan)).willReturn(Optional.empty());

			// when & then
			assertThatThrownBy(() -> membershipService.getActiveMembershipProduct())
				.isInstanceOf(MemberServiceApiException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.MEMBERSHIP_PRODUCT_NOT_FOUND);
		}
	}

	private MembershipPlan buildPlan() {
		return MembershipPlan.builder()
			.id(1L)
			.name("머브크루")
			.monthlyPrice(5000)
			.iosProductId("com.mub.app.membership.monthly")
			.androidProductId("mub_membership_monthly")
			.benefits(List.of("공간 예약 할인", "모임 이용권"))
			.active(true)
			.build();
	}

	private MembershipCohort buildCohort(MembershipPlan plan, LocalDate start, LocalDate end) {
		return MembershipCohort.builder()
			.id(1L)
			.plan(plan)
			.cohortNumber(3)
			.recruitStartDate(start)
			.recruitEndDate(end)
			.active(true)
			.build();
	}
}
