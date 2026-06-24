package com.yeolcheong.mub.member.dto;

import java.util.List;

import com.yeolcheong.mub.member.domain.InterestOption;
import com.yeolcheong.mub.member.domain.InterestType;
import com.yeolcheong.mub.member.domain.Member;
import com.yeolcheong.mub.member.domain.MemberInterest;
import com.yeolcheong.mub.member.domain.MemberInterestOption;
import com.yeolcheong.mub.member.domain.MemberStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class ProfileResponse {

	private Long memberId;
	private String nickname;
	private String bio;
	private Long imageId;
	private String regionProvince;
	private String regionCity;
	private MemberStatus status;
	private List<InterestInfo> interests;

	public static ProfileResponse from(Member member, List<MemberInterest> interests) {
		return ProfileResponse.builder()
			.memberId(member.getId())
			.nickname(member.getNickname())
			.bio(member.getBio())
			.imageId(member.getImageId())
			.regionProvince(member.getRegionProvince())
			.regionCity(member.getRegionCity())
			.status(member.getStatus())
			.interests(interests.stream().map(InterestInfo::from).toList())
			.build();
	}

	@Getter
	@Builder
	@AllArgsConstructor
	public static class InterestInfo {

		private InterestType interestType;
		private List<InterestOption> options;

		public static InterestInfo from(MemberInterest interest) {
			return InterestInfo.builder()
				.interestType(interest.getInterestType())
				.options(interest.getOptions().stream()
					.map(MemberInterestOption::getOptionType)
					.toList())
				.build();
		}
	}
}
