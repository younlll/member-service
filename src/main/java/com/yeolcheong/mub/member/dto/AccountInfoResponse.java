package com.yeolcheong.mub.member.dto;

import java.time.LocalDateTime;

import com.yeolcheong.mub.member.domain.Member;
import com.yeolcheong.mub.member.domain.SnsProvider;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * 내 계정 정보(연결된 소셜 계정) 응답.
 * 마이페이지 '계정정보'에서 연결된 카카오 계정을 표시하는 데 사용한다.
 */
@Getter
@Builder
@AllArgsConstructor
public class AccountInfoResponse {

	private Long memberId;
	private String email;
	private SnsProvider snsProvider;
	private String socialId;
	private LocalDateTime createdAt;

	public static AccountInfoResponse from(Member member) {
		return AccountInfoResponse.builder()
			.memberId(member.getId())
			.email(member.getEmail())
			.snsProvider(member.getSnsProvider())
			.socialId(member.getSocialId())
			.createdAt(member.getCreatedAt())
			.build();
	}
}
