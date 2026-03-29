package com.yeolcheong.mub.member.service;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yeolcheong.mub.member.common.MemberStatus;
import com.yeolcheong.mub.member.common.SnsProvider;
import com.yeolcheong.mub.member.domain.Member;
import com.yeolcheong.mub.member.dto.MemberInfoResponse;
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

	/**
	 * Create and persist a new Member using information from an SNS user response.
	 *
	 * <p>The created Member will use the KAKAO provider, the SNS user's Kakao ID as the social ID,
	 * the provided email, an INACTIVE status, and the current time as the last login time.</p>
	 *
	 * @param snsUserInfoResponse the SNS user information used to populate the new Member
	 * @return the persisted Member with its generated identifier populated
	 */
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

	/**
	 * Retrieve member information by email.
	 *
	 * @param email the email address of the member to look up
	 * @return a MemberInfoResponse representing the found member
	 * @throws MemberServiceApiException if no member exists for the given email (ErrorCode.MEMBER_NOT_FOUND)
	 */
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
}
