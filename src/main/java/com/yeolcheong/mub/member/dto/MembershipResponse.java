package com.yeolcheong.mub.member.dto;

import java.time.LocalDateTime;

import com.yeolcheong.mub.member.domain.Membership;
import com.yeolcheong.mub.member.domain.MembershipPlatform;
import com.yeolcheong.mub.member.domain.MembershipStatus;

import lombok.Builder;
import lombok.Getter;

/**
 * 회원 멤버십(구독) 응답. 구매검증 결과와 내 멤버십 조회에 공통 사용한다.
 */
@Getter
@Builder
public class MembershipResponse {

	private Long membershipId;
	private Long memberId;
	private String planName;
	private int cohortNumber;
	private MembershipPlatform platform;
	private MembershipStatus status;
	private LocalDateTime startedAt;
	private LocalDateTime expiresAt;

	public static MembershipResponse from(Membership membership) {
		return MembershipResponse.builder()
			.membershipId(membership.getId())
			.memberId(membership.getMemberId())
			.planName(membership.getCohort().getPlan().getName())
			.cohortNumber(membership.getCohort().getCohortNumber())
			.platform(membership.getPlatform())
			.status(membership.getStatus())
			.startedAt(membership.getStartedAt())
			.expiresAt(membership.getExpiresAt())
			.build();
	}
}
