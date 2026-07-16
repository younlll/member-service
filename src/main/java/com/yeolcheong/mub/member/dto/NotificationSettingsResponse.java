package com.yeolcheong.mub.member.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * 알림 설정 응답.
 * 현재는 광고성(마케팅) 수신 동의 여부만 노출한다.
 */
@Getter
@Builder
@AllArgsConstructor
public class NotificationSettingsResponse {

	private boolean marketingAgreed;

	public static NotificationSettingsResponse of(boolean marketingAgreed) {
		return NotificationSettingsResponse.builder()
			.marketingAgreed(marketingAgreed)
			.build();
	}
}
