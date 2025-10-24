package com.member.service;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.member.common.MemberStatus;
import com.member.common.SnsProvider;
import com.member.domain.Member;
import com.member.dto.SnsUserInfoResponse;
import com.member.exception.ErrorCode;
import com.member.exception.MemberServiceApiException;
import com.member.repository.MemberRepository;

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
}
