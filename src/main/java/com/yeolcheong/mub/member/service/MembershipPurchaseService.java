package com.yeolcheong.mub.member.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

import lombok.extern.slf4j.Slf4j;

/**
 * 멤버십 구매검증 서비스. 플랫폼별 검증기로 스토어 구매를 검증하고 멤버십 엔타이틀먼트를 생성/갱신한다.
 */
@Service
@Transactional(readOnly = true)
@Slf4j
public class MembershipPurchaseService {

	private final Map<MembershipPlatform, StorePurchaseVerifier> verifiers;
	private final MembershipPlanRepository planRepository;
	private final MembershipCohortRepository cohortRepository;
	private final MembershipRepository membershipRepository;

	public MembershipPurchaseService(
		List<StorePurchaseVerifier> verifierList,
		MembershipPlanRepository planRepository,
		MembershipCohortRepository cohortRepository,
		MembershipRepository membershipRepository) {
		this.verifiers = verifierList.stream()
			.collect(Collectors.toMap(StorePurchaseVerifier::platform, Function.identity()));
		this.planRepository = planRepository;
		this.cohortRepository = cohortRepository;
		this.membershipRepository = membershipRepository;
	}

	/**
	 * 스토어 구매를 검증하고 멤버십을 활성화한다. 동일 거래의 재검증은 멱등 처리(기존 멤버십 갱신).
	 *
	 * @param memberId 요청 회원 ID
	 * @param request  플랫폼·상품ID·구매토큰
	 * @return 활성화된 멤버십
	 * @throws MemberServiceApiException 미지원 플랫폼/검증 실패/상품 불일치/상품 없음 시
	 */
	@Transactional
	public MembershipResponse verifyAndActivate(Long memberId, MembershipPurchaseRequest request) {
		StorePurchaseVerifier verifier = verifiers.get(request.getPlatform());
		if (verifier == null) {
			throw new MemberServiceApiException(ErrorCode.UNSUPPORTED_PLATFORM);
		}

		StoreVerificationResult result = verifier.verify(request.getProductId(), request.getPurchaseToken());
		if (!result.isValid()) {
			throw new MemberServiceApiException(ErrorCode.MEMBERSHIP_VERIFICATION_FAILED);
		}

		// 멱등: 이미 검증된 거래면 만료일만 갱신하고 반환
		Optional<Membership> existing = membershipRepository.findByStoreTransactionId(result.getStoreTransactionId());
		if (existing.isPresent()) {
			Membership membership = existing.get();
			membership.renew(MembershipStatus.ACTIVE, result.getExpiresAt());
			log.info("Membership renewed (idempotent) | memberId={}, txId={}", memberId, result.getStoreTransactionId());
			return MembershipResponse.from(membership);
		}

		MembershipPlan plan = planRepository.findFirstByActiveTrue()
			.orElseThrow(() -> new MemberServiceApiException(ErrorCode.MEMBERSHIP_PRODUCT_NOT_FOUND));
		String expectedProductId = request.getPlatform() == MembershipPlatform.APPLE
			? plan.getIosProductId() : plan.getAndroidProductId();
		if (!expectedProductId.equals(result.getProductId())) {
			log.warn("Product mismatch | expected={}, actual={}", expectedProductId, result.getProductId());
			throw new MemberServiceApiException(ErrorCode.MEMBERSHIP_PRODUCT_MISMATCH);
		}
		MembershipCohort cohort = cohortRepository.findFirstByPlanOrderByCohortNumberDesc(plan)
			.orElseThrow(() -> new MemberServiceApiException(ErrorCode.MEMBERSHIP_PRODUCT_NOT_FOUND));

		Membership membership = Membership.builder()
			.memberId(memberId)
			.cohort(cohort)
			.platform(request.getPlatform())
			.storeTransactionId(result.getStoreTransactionId())
			.productId(result.getProductId())
			.status(MembershipStatus.ACTIVE)
			.startedAt(LocalDateTime.now())
			.expiresAt(result.getExpiresAt())
			.build();
		membershipRepository.save(membership);

		log.info("Membership activated | memberId={}, cohort={}, platform={}",
			memberId, cohort.getCohortNumber(), request.getPlatform());
		return MembershipResponse.from(membership);
	}
}
