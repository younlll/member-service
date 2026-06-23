package com.yeolcheong.mub.member.service;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yeolcheong.mub.member.domain.MemberStatus;
import com.yeolcheong.mub.member.domain.SnsProvider;
import com.yeolcheong.mub.member.domain.Member;
import com.yeolcheong.mub.member.dto.MemberInfoResponse;
import com.yeolcheong.mub.member.dto.MemberSummaryResponse;
import com.yeolcheong.mub.member.dto.SnsUserInfoResponse;
import com.yeolcheong.mub.member.exception.ErrorCode;
import com.yeolcheong.mub.member.exception.MemberServiceApiException;
import com.yeolcheong.mub.member.repository.MemberRepository;
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

	public Member findById(Long memberId) {
		return memberRepository.findById(memberId)
			.orElseThrow(() -> new MemberServiceApiException(ErrorCode.MEMBER_NOT_FOUND));
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
	 */
	public List<MemberSummaryResponse> findSummariesByIds(Collection<Long> memberIds) {
		if (memberIds == null || memberIds.isEmpty()) {
			return Collections.emptyList();
		}
		log.info("Bulk member lookup: requestedSize={}", memberIds.size());

		List<MemberSummaryResponse> results = memberRepository.findAllById(memberIds).stream()
			.map(MemberSummaryResponse::from)
			.toList();

		log.info("Bulk member lookup completed: requestedSize={}, foundSize={}", memberIds.size(), results.size());
		return results;
	}
}
