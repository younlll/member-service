package com.yeolcheong.mub.member.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yeolcheong.mub.member.domain.District;
import com.yeolcheong.mub.member.domain.InterestOption;
import com.yeolcheong.mub.member.domain.InterestType;
import com.yeolcheong.mub.member.domain.Member;
import com.yeolcheong.mub.member.domain.MemberInterest;
import com.yeolcheong.mub.member.domain.MemberStatus;
import com.yeolcheong.mub.member.domain.MemberTermsAgreement;
import com.yeolcheong.mub.member.domain.SnsProvider;
import com.yeolcheong.mub.member.domain.TermsType;
import com.yeolcheong.mub.member.dto.AccountInfoResponse;
import com.yeolcheong.mub.member.dto.MemberInfoResponse;
import com.yeolcheong.mub.member.dto.MemberSummaryResponse;
import com.yeolcheong.mub.member.dto.NotificationSettingsResponse;
import com.yeolcheong.mub.member.dto.ProfileResponse;
import com.yeolcheong.mub.member.dto.ProfileUpdateRequest;
import com.yeolcheong.mub.member.dto.SnsUserInfoResponse;
import com.yeolcheong.mub.member.exception.ErrorCode;
import com.yeolcheong.mub.member.exception.MemberServiceApiException;
import com.yeolcheong.mub.member.repository.DistrictRepository;
import com.yeolcheong.mub.member.repository.MemberInterestRepository;
import com.yeolcheong.mub.member.repository.MemberRepository;
import com.yeolcheong.mub.member.repository.MemberTermsAgreementRepository;
import com.yeolcheong.mub.member.repository.RefreshTokenRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class MemberService {

	private final MemberRepository memberRepository;
	private final RefreshTokenRepository refreshTokenRepository;
	private final DistrictRepository districtRepository;
	private final MemberInterestRepository memberInterestRepository;
	private final MemberTermsAgreementRepository memberTermsAgreementRepository;
	private final ProfileImageService profileImageService;

	public Member findById(Long memberId) {
		return memberRepository.findById(memberId)
			.orElseThrow(() -> new MemberServiceApiException(ErrorCode.MEMBER_NOT_FOUND));
	}

	/**
	 * 탈퇴(DELETED) 회원 재가입 처리.
	 * <p>
	 * 기존 관심사·약관 동의 이력을 정리하고, 프로필을 초기화한 뒤 상태를 {@code INACTIVE}로 되돌린다.
	 * 이후 호출자는 신규 회원과 동일하게 온보딩을 진행시킨다.
	 *
	 * @param member 재활성화할 탈퇴 회원
	 */
	@Transactional
	public void reactivateForResignup(Member member) {
		memberInterestRepository.deleteByMemberId(member.getId());
		memberTermsAgreementRepository.deleteByMemberId(member.getId());
		member.reactivate();
		memberRepository.save(member);

		log.info("Withdrawn member reactivated for re-signup: memberId={}", member.getId());
	}

	/**
	 * 회원 탈퇴(soft delete).
	 * <p>
	 * 회원 상태를 {@code DELETED}로 전환하고, 저장된 리프레시 토큰을 무효화해 재발급을 차단한다.
	 * 레코드 자체는 보존한다(다른 서비스 참조·이력 유지). 이미 탈퇴한 회원이면 거절한다.
	 *
	 * @param memberId 인증된 본인 회원 ID
	 */
	@Transactional
	public void withdraw(Long memberId) {
		Member member = memberRepository.findById(memberId)
			.orElseThrow(() -> new MemberServiceApiException(ErrorCode.MEMBER_NOT_FOUND));

		if (MemberStatus.DELETED.equals(member.getStatus())) {
			throw new MemberServiceApiException(ErrorCode.ALREADY_WITHDRAWN);
		}

		member.updateMemberState(MemberStatus.DELETED);
		refreshTokenRepository.deleteByMemberId(memberId);

		log.info("Member withdrawn: memberId={}", memberId);
	}

	/**
	 * 내 프로필 조회 (닉네임/한줄소개/지역/관심사 포함).
	 */
	public ProfileResponse getMyProfile(Long memberId) {
		log.info("Profile retrieval requested: memberId={}", memberId);

		Member member = findById(memberId);
		List<MemberInterest> interests = memberInterestRepository.findAllByMemberId(memberId);

		return ProfileResponse.from(member, interests);
	}

	/**
	 * 내 계정 정보(연결된 소셜 계정) 조회.
	 * 마이페이지 '계정정보'에서 카카오 연결 계정을 표시하는 데 사용한다.
	 */
	public AccountInfoResponse getMyAccount(Long memberId) {
		return AccountInfoResponse.from(findById(memberId));
	}

	/**
	 * 알림 설정(광고성 수신 동의) 조회. 동의 이력이 없으면 미동의(false)로 본다.
	 */
	public NotificationSettingsResponse getNotificationSettings(Long memberId) {
		boolean marketingAgreed = memberTermsAgreementRepository
			.findByMemberIdAndTermsType(memberId, TermsType.MARKETING)
			.map(MemberTermsAgreement::getAgreed)
			.orElse(false);

		return NotificationSettingsResponse.of(marketingAgreed);
	}

	/**
	 * 알림 설정(광고성 수신 동의) 수정(토글). 기존 동의 이력이 있으면 갱신하고, 없으면 새로 생성한다.
	 */
	@Transactional
	public NotificationSettingsResponse updateNotificationSettings(Long memberId, boolean marketingAgreed) {
		Member member = findById(memberId);

		MemberTermsAgreement agreement = memberTermsAgreementRepository
			.findByMemberIdAndTermsType(memberId, TermsType.MARKETING)
			.orElse(null);

		if (agreement == null) {
			agreement = MemberTermsAgreement.builder()
				.member(member)
				.termsType(TermsType.MARKETING)
				.agreed(marketingAgreed)
				.build();
		} else {
			agreement.updateAgreed(marketingAgreed);
		}
		memberTermsAgreementRepository.save(agreement);

		log.info("Notification settings updated: memberId={}, marketingAgreed={}", memberId, marketingAgreed);
		return NotificationSettingsResponse.of(marketingAgreed);
	}

	/**
	 * 내 프로필 수정 (닉네임, 한줄소개, 활동 지역, 관심사).
	 */
	@Transactional
	public ProfileResponse updateProfile(Long memberId, ProfileUpdateRequest request) {
		log.info("Profile update requested: memberId={}", memberId);

		Member member = findById(memberId);
		if (MemberStatus.DELETED.equals(member.getStatus())) {
			throw new MemberServiceApiException(ErrorCode.MEMBER_NOT_FOUND);
		}

		member.updateNickname(request.getNickname());
		member.updateBio(request.getBio());

		District district = districtRepository.findByDistCode1AndDistCode2(
				request.getDistCode1(), request.getDistCode2())
			.orElseThrow(() -> new MemberServiceApiException(ErrorCode.INVALID_DISTRICT_CODE));
		member.updateRegion(district.getDistCode1Name(), district.getDistCode2Name());

		List<MemberInterest> savedInterests = replaceInterests(member, request.getInterests());
		memberRepository.save(member);

		log.info("Profile update succeeded: memberId={}", memberId);
		return ProfileResponse.from(member, savedInterests);
	}

	/**
	 * 관심사 목록을 요청값으로 교체한다. 요청에 없는 기존 관심사는 삭제하고,
	 * 있는 항목은 옵션을 갱신하며, 새 항목은 생성한다.
	 */
	private List<MemberInterest> replaceInterests(Member member, List<ProfileUpdateRequest.InterestRequest> requests) {
		List<MemberInterest> existing = memberInterestRepository.findAllByMemberId(member.getId());
		Map<InterestType, MemberInterest> existingMap = existing.stream()
			.collect(Collectors.toMap(MemberInterest::getInterestType, mi -> mi));

		Set<InterestType> requestedTypes = requests.stream()
			.map(ProfileUpdateRequest.InterestRequest::getInterestType)
			.collect(Collectors.toSet());

		existing.stream()
			.filter(mi -> !requestedTypes.contains(mi.getInterestType()))
			.forEach(memberInterestRepository::delete);

		List<MemberInterest> result = new ArrayList<>();
		for (ProfileUpdateRequest.InterestRequest request : requests) {
			validateInterestOptions(request.getInterestType(), request.getOptions());

			MemberInterest interest = existingMap.get(request.getInterestType());
			if (interest != null) {
				interest.replaceOptions(request.getOptions());
			} else {
				interest = MemberInterest.builder()
					.member(member)
					.interestType(request.getInterestType())
					.build();
				request.getOptions().forEach(interest::addOption);
			}
			result.add(memberInterestRepository.save(interest));
		}
		return result;
	}

	/**
	 * 관심사 옵션 유효성 검사 (최소 1개 + 해당 관심사 허용 옵션 여부).
	 */
	private void validateInterestOptions(InterestType interestType, List<InterestOption> options) {
		if (options == null || options.isEmpty()) {
			throw new MemberServiceApiException("관심사별 옵션은 최소 1개 이상 선택해야 합니다", ErrorCode.INTEREST_OPTION_REQUIRED);
		}

		List<InterestOption> available = interestType.getAvailableOptions();
		for (InterestOption option : options) {
			if (!available.contains(option)) {
				throw new MemberServiceApiException(
					String.format("'%s' 관심사에는 '%s' 옵션을 선택할 수 없습니다",
						interestType.getDescription(), option.getDescription()),
					ErrorCode.INVALID_INTEREST_OPTION);
			}
		}
	}

	public Optional<Member> findBySocialId(SnsProvider snsProvider, String socialId) {
		return memberRepository.findBySnsProviderAndSocialId(snsProvider, socialId);
	}

	@Transactional
	public Member createdFromSnsUser(SnsUserInfoResponse snsUserInfoResponse) {
		log.info("Creating new member: snsId={}, email={}", snsUserInfoResponse.getId(), snsUserInfoResponse.getEmail());

		Member member = Member.builder()
			.snsProvider(SnsProvider.KAKAO)
			.socialId(snsUserInfoResponse.getKakaoIdAsString())
			.email(snsUserInfoResponse.getEmail())
			.status(MemberStatus.INACTIVE)
			.lastLoginAt(LocalDateTime.now())
			.build();

		Member saveMember = memberRepository.save(member);
		log.info("New member created: memberId={}", saveMember.getId());

		return saveMember;
	}

	public MemberInfoResponse getMemberByEmail(String email) {
		log.info("Looking up member by email: email={}", email);

		Member member = memberRepository.findByEmail(email)
			.orElseThrow(() -> {
				log.warn("Member lookup failed - email not found: email={}", email);
				return new MemberServiceApiException("존재하지 않는 회원입니다.", ErrorCode.MEMBER_NOT_FOUND);
			});

		log.info("Member lookup succeeded: memberId={}", member.getId());
		return MemberInfoResponse.from(member);
	}

	/**
	 * Bulk lookup for service-to-service enrichment.
	 * Returns only the members that actually exist — callers must tolerate
	 * a result smaller than the requested id set (e.g. deleted members).
	 * Each summary carries a resolved public profile image URL (default image when none).
	 */
	public List<MemberSummaryResponse> findSummariesByIds(Collection<Long> memberIds) {
		if (memberIds == null || memberIds.isEmpty()) {
			return Collections.emptyList();
		}
		log.info("Bulk member lookup: requestedSize={}", memberIds.size());

		List<Member> members = memberRepository.findAllById(memberIds);
		Map<Long, String> imageUrls = profileImageService.resolveImageUrls(members);

		List<MemberSummaryResponse> results = members.stream()
			.map(member -> MemberSummaryResponse.from(member, imageUrls.get(member.getId())))
			.toList();

		log.info("Bulk member lookup completed: requestedSize={}, foundSize={}", memberIds.size(), results.size());
		return results;
	}
}
