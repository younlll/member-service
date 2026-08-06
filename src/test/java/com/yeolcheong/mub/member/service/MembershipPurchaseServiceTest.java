package com.yeolcheong.mub.member.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.yeolcheong.mub.member.client.StorePurchaseVerifier;
import com.yeolcheong.mub.member.client.StoreVerificationResult;
import com.yeolcheong.mub.member.domain.Membership;
import com.yeolcheong.mub.member.domain.MembershipCohort;
import com.yeolcheong.mub.member.domain.MembershipPlan;
import com.yeolcheong.mub.member.domain.MembershipPlatform;
import com.yeolcheong.mub.member.domain.MembershipStatus;
import com.yeolcheong.mub.member.dto.MembershipPurchaseRequest;
import com.yeolcheong.mub.member.dto.MembershipResponse;
import com.yeolcheong.mub.member.exception.ErrorCode;
import com.yeolcheong.mub.member.exception.MemberServiceApiException;
import com.yeolcheong.mub.member.repository.MembershipCohortRepository;
import com.yeolcheong.mub.member.repository.MembershipPlanRepository;
import com.yeolcheong.mub.member.repository.MembershipRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("MembershipPurchaseService")
class MembershipPurchaseServiceTest {

	private static final String IOS_PRODUCT_ID = "com.mub.app.membership.monthly";

	@Mock
	private StorePurchaseVerifier appleVerifier;
	@Mock
	private MembershipPlanRepository planRepository;
	@Mock
	private MembershipCohortRepository cohortRepository;
	@Mock
	private MembershipRepository membershipRepository;

	private MembershipPurchaseService service;

	@BeforeEach
	void setUp() {
		given(appleVerifier.platform()).willReturn(MembershipPlatform.APPLE);
		service = new MembershipPurchaseService(
			List.of(appleVerifier), planRepository, cohortRepository, membershipRepository);
	}

	@Test
	@DisplayName("verifyAndActivate - activates a new membership when the purchase is valid")
	void activatesNewMembership() {
		// given
		given(appleVerifier.verify(IOS_PRODUCT_ID, "tok")).willReturn(validResult());
		given(membershipRepository.findByStoreTransactionId("txn_1")).willReturn(Optional.empty());
		MembershipPlan plan = buildPlan();
		given(planRepository.findFirstByActiveTrue()).willReturn(Optional.of(plan));
		given(cohortRepository.findFirstByPlanOrderByCohortNumberDesc(plan)).willReturn(Optional.of(buildCohort(plan)));

		// when
		MembershipResponse response = service.verifyAndActivate(1L, request(MembershipPlatform.APPLE, IOS_PRODUCT_ID));

		// then
		assertThat(response.getStatus()).isEqualTo(MembershipStatus.ACTIVE);
		assertThat(response.getCohortNumber()).isEqualTo(3);
		then(membershipRepository).should().save(any(Membership.class));
	}

	@Test
	@DisplayName("verifyAndActivate - is idempotent when the store transaction already exists")
	void idempotentOnExistingTransaction() {
		// given
		MembershipPlan plan = buildPlan();
		Membership existing = Membership.builder()
			.id(9L).memberId(1L).cohort(buildCohort(plan)).platform(MembershipPlatform.APPLE)
			.storeTransactionId("txn_1").productId(IOS_PRODUCT_ID).status(MembershipStatus.ACTIVE)
			.startedAt(LocalDateTime.now().minusDays(1)).expiresAt(LocalDateTime.now().plusDays(1)).build();
		given(appleVerifier.verify(IOS_PRODUCT_ID, "tok")).willReturn(validResult());
		given(membershipRepository.findByStoreTransactionId("txn_1")).willReturn(Optional.of(existing));

		// when
		MembershipResponse response = service.verifyAndActivate(1L, request(MembershipPlatform.APPLE, IOS_PRODUCT_ID));

		// then — existing renewed, no new save
		assertThat(response.getMembershipId()).isEqualTo(9L);
		assertThat(response.getStatus()).isEqualTo(MembershipStatus.ACTIVE);
		then(membershipRepository).should(never()).save(any());
	}

	@Test
	@DisplayName("verifyAndActivate - throws MEMBERSHIP_VERIFICATION_FAILED when the store rejects the purchase")
	void throwsWhenVerificationInvalid() {
		given(appleVerifier.verify(IOS_PRODUCT_ID, "tok")).willReturn(StoreVerificationResult.invalid());

		assertThatThrownBy(() -> service.verifyAndActivate(1L, request(MembershipPlatform.APPLE, IOS_PRODUCT_ID)))
			.isInstanceOf(MemberServiceApiException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.MEMBERSHIP_VERIFICATION_FAILED);
	}

	@Test
	@DisplayName("verifyAndActivate - throws MEMBERSHIP_PRODUCT_MISMATCH when verified product differs from the plan")
	void throwsWhenProductMismatch() {
		StoreVerificationResult wrong = StoreVerificationResult.builder()
			.valid(true).productId("com.other.product").storeTransactionId("txn_1")
			.expiresAt(LocalDateTime.now().plusDays(30)).autoRenewing(true).build();
		given(appleVerifier.verify("com.other.product", "tok")).willReturn(wrong);
		given(membershipRepository.findByStoreTransactionId("txn_1")).willReturn(Optional.empty());
		given(planRepository.findFirstByActiveTrue()).willReturn(Optional.of(buildPlan()));

		assertThatThrownBy(() -> service.verifyAndActivate(1L, request(MembershipPlatform.APPLE, "com.other.product")))
			.isInstanceOf(MemberServiceApiException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.MEMBERSHIP_PRODUCT_MISMATCH);
	}

	@Test
	@DisplayName("verifyAndActivate - throws UNSUPPORTED_PLATFORM when no verifier handles the platform")
	void throwsWhenUnsupportedPlatform() {
		// only APPLE verifier registered; request GOOGLE
		assertThatThrownBy(() -> service.verifyAndActivate(1L, request(MembershipPlatform.GOOGLE, "mub_membership_monthly")))
			.isInstanceOf(MemberServiceApiException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.UNSUPPORTED_PLATFORM);
	}

	private StoreVerificationResult validResult() {
		return StoreVerificationResult.builder()
			.valid(true).productId(IOS_PRODUCT_ID).storeTransactionId("txn_1")
			.expiresAt(LocalDateTime.now().plusDays(30)).autoRenewing(true).build();
	}

	private MembershipPurchaseRequest request(MembershipPlatform platform, String productId) {
		return MembershipPurchaseRequest.builder()
			.platform(platform).productId(productId).purchaseToken("tok").build();
	}

	private MembershipPlan buildPlan() {
		return MembershipPlan.builder()
			.id(1L).name("머브크루").monthlyPrice(5000)
			.iosProductId(IOS_PRODUCT_ID).androidProductId("mub_membership_monthly")
			.active(true).build();
	}

	private MembershipCohort buildCohort(MembershipPlan plan) {
		return MembershipCohort.builder()
			.id(1L).plan(plan).cohortNumber(3)
			.recruitStartDate(LocalDate.now().minusDays(1)).recruitEndDate(LocalDate.now().plusDays(10))
			.active(true).build();
	}
}
