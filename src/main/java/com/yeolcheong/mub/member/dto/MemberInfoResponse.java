package com.yeolcheong.mub.member.dto;

import com.yeolcheong.mub.member.common.MemberStatus;
import com.yeolcheong.mub.member.common.SnsProvider;
import com.yeolcheong.mub.member.domain.Member;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class MemberInfoResponse {

	private Long memberId;
	private String email;
	private String nickname;
	private SnsProvider snsProvider;
	private String socialId;
	private String regionProvince;
	private String regionCity;
	private MemberStatus status;

	public static MemberInfoResponse from(Member member) {
		return MemberInfoResponse.builder()
			.memberId(member.getId())
			.email(member.getEmail())
			.nickname(member.getNickname())
			.snsProvider(member.getSnsProvider())
			.socialId(member.getSocialId())
			.regionProvince(member.getRegionProvince())
			.regionCity(member.getRegionCity())
			.status(member.getStatus())
			.build();
	}
}
