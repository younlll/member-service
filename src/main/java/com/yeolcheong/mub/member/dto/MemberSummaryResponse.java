package com.yeolcheong.mub.member.dto;

import com.yeolcheong.mub.member.domain.Member;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * Lightweight member view for internal service-to-service lookups.
 * Intentionally narrower than {@link MemberInfoResponse} — exposes only
 * what other services need to enrich their own data (id, nickname, email).
 */
@Getter
@Builder
@AllArgsConstructor
public class MemberSummaryResponse {

	private Long memberId;
	private String nickname;
	private String email;

	public static MemberSummaryResponse from(Member member) {
		return MemberSummaryResponse.builder()
			.memberId(member.getId())
			.nickname(member.getNickname())
			.email(member.getEmail())
			.build();
	}
}
