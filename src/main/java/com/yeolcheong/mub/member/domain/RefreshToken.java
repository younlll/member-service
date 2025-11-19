package com.yeolcheong.mub.member.domain;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {

	private Long memberId;
	private String token;
	private LocalDateTime expiresAt;

	public boolean isExpired() {
		return LocalDateTime.now().isAfter(expiresAt);
	}
}
