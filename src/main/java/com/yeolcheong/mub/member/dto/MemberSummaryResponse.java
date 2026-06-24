package com.yeolcheong.mub.member.dto;

import com.yeolcheong.mub.member.domain.Member;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * Lightweight member view for internal service-to-service lookups.
 * Intentionally narrower than {@link MemberInfoResponse} — exposes only
 * what other services need to enrich their own data (id, nickname,
 * profile image, email).
 */
@Getter
@Builder
@AllArgsConstructor
public class MemberSummaryResponse {

	private Long memberId;
	private String nickname;
	private String profileImageUrl;
	private String email;

	/**
	 * Builds a summary with a pre-resolved public profile image URL.
	 *
	 * @param member          source member
	 * @param profileImageUrl resolved public image URL (default image when none)
	 * @return member summary
	 */
	public static MemberSummaryResponse from(Member member, String profileImageUrl) {
		return MemberSummaryResponse.builder()
			.memberId(member.getId())
			.nickname(member.getNickname())
			.profileImageUrl(profileImageUrl)
			.email(member.getEmail())
			.build();
	}
}
