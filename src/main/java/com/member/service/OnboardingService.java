package com.member.service;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.member.common.MemberStatus;
import com.member.domain.District;
import com.member.domain.InterestOption;
import com.member.domain.InterestType;
import com.member.domain.Member;
import com.member.domain.MemberInterest;
import com.member.domain.MemberTermsAgreement;
import com.member.domain.TermsType;
import com.member.dto.OnboardingRequest;
import com.member.dto.OnboardingResponse;
import com.member.exception.ErrorCode;
import com.member.exception.MemberServiceApiException;
import com.member.exception.OnboardingServiceApiException;
import com.member.repository.DistrictRepository;
import com.member.repository.MemberInterestRepository;
import com.member.repository.MemberRepository;
import com.member.repository.MemberTermsAgreementRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class OnboardingService {

	private final MemberRepository memberRepository;
	private final MemberTermsAgreementRepository memberTermsAgreementRepository;
	private final DistrictRepository districtRepository;
	private final MemberInterestRepository memberInterestRepository;

	@Transactional
	public OnboardingResponse completeOnboarding(Long memberId, OnboardingRequest onboardingRequest) {
		log.info("회원가입 시작: memberId={}", memberId);

		// 1. 회원 조회
		Member member = memberRepository.findById(memberId)
			.orElseThrow(() -> new MemberServiceApiException(ErrorCode.MEMBER_NOT_FOUND));

		// 2. 회원가입한 회원인지 확인
		if (MemberStatus.ACTIVE.equals(member.getStatus()) || MemberStatus.MUBACTIVE.equals(member.getStatus())) {
			throw new MemberServiceApiException(ErrorCode.ALREADY_ONBOARDED);
		}

		// 3. 약관 동의 저장
		saveTermsAgreements(member, onboardingRequest.getTermsAgreementRequest());

		// 4. 닉네임 설정
		updateNickname(member, onboardingRequest.getNickname());

		// 5. 활동 지역 설정
		updateRegion(member, onboardingRequest.getDistCode1(), onboardingRequest.getDistCode2());

		// 6. 관심사 저장
		saveInterests(member, onboardingRequest.getInterests());

		// 7. 온보딩 완료 처리
		member.updateMemberState(MemberStatus.ACTIVE);
		memberRepository.save(member);

		return OnboardingResponse.of(member.getId(), member.getNickname());
	}

	/**
	 * 약관 동의 저장
	 */
	private void saveTermsAgreements(Member member, OnboardingRequest.TermsAgreementRequest termsAgreementRequest) {
		log.debug("약관 동의 저장 시작: memberId={}", member.getId());

		Map<TermsType, Boolean> agreements = termsAgreementRequest.toMap();

		// 필수 약관 동의 확인
		for (TermsType termsType : TermsType.values()) {
			if (termsType.isRequired()) {
				Boolean agreed = agreements.get(termsType);
				if (agreed == null || !agreed) {
					throw new MemberServiceApiException(termsType.getDescription() + "에 동의해야 합니다",
						ErrorCode.TERMS_AGREEMENT_REQUIRED);
				}
			}
		}

		// 약관 동의 저장
		List<MemberTermsAgreement> memberTermsAgreements = agreements.entrySet().stream()
			.map(entry -> MemberTermsAgreement.builder()
				.member(member)
				.termsType(entry.getKey())
				.agreed(entry.getValue())
				.build())
			.toList();

		memberTermsAgreementRepository.saveAll(memberTermsAgreements);
		log.debug("약관 동의 저장 완료: memberId={}, count={}", member.getId(), memberTermsAgreements.size());
	}

	/**
	 * 닉네임 설정
	 */
	private void updateNickname(Member member, String nickname) {
		log.debug("닉네임 설정 시작: memberId={}, nickname={}", member.getId(), nickname);

		member.updateNickname(nickname);
		log.debug("닉네임 설정 완료: memberId={}, nickname={}", member.getId(), nickname);
	}

	/**
	 * 활동 지역 설정
	 */
	private void updateRegion(Member member, String distCode1, String distCode2) {
		log.debug("활동 지역 설정 시작: memberId={}, distCode1={}, distCode2={}", member.getId(), distCode1, distCode2);

		// 지역 코드 유효성 확인
		District district = districtRepository.findByDistCode1AndDistCode2(distCode1, distCode2)
			.orElseThrow(() -> new OnboardingServiceApiException("유효하지 않은 지역 코드입니다"));

		member.updateRegion(district.getDistCode1Name(), district.getDistCode2Name());

		log.debug("활동 지역 설정 완료: memberId={}, region={} {}",
			member.getId(), district.getDistCode1Name(), district.getDistCode2Name());
	}

	/**
	 * 관심사 저장
	 */
	private void saveInterests(Member member, List<OnboardingRequest.InterestRequest> interestRequests) {
		log.debug("관심사 저장 시작: memberId={}, count={}", member.getId(), interestRequests.size());

		if (interestRequests.isEmpty() || interestRequests.size() > 3) {
			throw new MemberServiceApiException("관심사는 1개 이상 3개 이하로 선택해야 합니다", ErrorCode.INVALID_INTEREST_COUNT);
		}

		memberInterestRepository.deleteByMemberId(member.getId());

		for (OnboardingRequest.InterestRequest interestRequest : interestRequests) {
			InterestType interestType = interestRequest.getInterestType();
			List<InterestOption> options = interestRequest.getOptions();

			validateInterestOptions(interestType, options);

			MemberInterest memberInterest = MemberInterest.builder()
				.member(member)
				.interestType(interestType)
				.build();

			options.forEach(memberInterest::addOption);

			memberInterestRepository.save(memberInterest);
		}
	}

	/**
	 * 관심사 옵션 유효성 검사
	 */
	private void validateInterestOptions(InterestType interestType, List<InterestOption> options) {
		if (options == null || options.isEmpty()) {
			throw new MemberServiceApiException("관심사별 옵션은 최소 1개 이상 선택해야 합니다", ErrorCode.INTEREST_OPTION_REQUIRED);
		}

		List<InterestOption> availableOptions = interestType.getAvailableOptions();
		for (InterestOption option : options) {
			if (!availableOptions.contains(option)) {
				throw new MemberServiceApiException(String.format("'%s' 관심사에는 '%s' 옵션을 선택할 수 없습니다",
					interestType.getDescription(), option.getDescription()), ErrorCode.INVALID_INTEREST_OPTION);
			}
		}
	}
}
