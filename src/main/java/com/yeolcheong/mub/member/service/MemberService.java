package com.yeolcheong.mub.member.service;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yeolcheong.mub.member.common.MemberStatus;
import com.yeolcheong.mub.member.common.SnsProvider;
import com.yeolcheong.mub.member.domain.Member;
import com.yeolcheong.mub.member.dto.MemberInfoResponse;
import com.yeolcheong.mub.member.dto.MemberSummaryResponse;
import com.yeolcheong.mub.member.dto.SnsUserInfoResponse;
import com.yeolcheong.mub.member.exception.ErrorCode;
import com.yeolcheong.mub.member.exception.MemberServiceApiException;
import com.yeolcheong.mub.member.repository.MemberRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class MemberService {

	private final MemberRepository memberRepository;

	public Member findById(Long memberId) {
		return memberRepository.findById(memberId)
			.orElseThrow(() -> new MemberServiceApiException(ErrorCode.MEMBER_NOT_FOUND));
	}

	public Optional<Member> findBySocialId(SnsProvider snsProvider, String socialId) {
		return memberRepository.findBySnsProviderAndSocialId(snsProvider, socialId);
	}

	public Member createdFromSnsUser(SnsUserInfoResponse snsUserInfoResponse) {
		log.info("신규 회원 생성: snsId={}, email={}", snsUserInfoResponse.getId(), snsUserInfoResponse.getEmail());

		Member member = Member.builder()
			.snsProvider(SnsProvider.KAKAO)
			.socialId(snsUserInfoResponse.getKakaoIdAsString())
			.email(snsUserInfoResponse.getEmail())
			.status(MemberStatus.INACTIVE)
			.lastLoginAt(LocalDateTime.now())
			.build();

		Member saveMember = memberRepository.save(member);
		log.info("신규 회원 생성 완료: memberId={}", saveMember.getId());

		return saveMember;
	}

	public MemberInfoResponse getMemberByEmail(String email) {
		log.info("이메일로 회원 조회: email={}", email);

		Member member = memberRepository.findByEmail(email)
			.orElseThrow(() -> {
				log.warn("회원 조회 실패 - 존재하지 않는 이메일: email={}", email);
				return new MemberServiceApiException("존재하지 않는 회원입니다.", ErrorCode.MEMBER_NOT_FOUND);
			});

		log.info("회원 조회 성공: memberId={}", member.getId());
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
		log.info("회원 다건 조회: requestedSize={}", memberIds.size());

		List<MemberSummaryResponse> results = memberRepository.findAllById(memberIds).stream()
			.map(MemberSummaryResponse::from)
			.toList();

		log.info("회원 다건 조회 완료: requestedSize={}, foundSize={}", memberIds.size(), results.size());
		return results;
	}
}
