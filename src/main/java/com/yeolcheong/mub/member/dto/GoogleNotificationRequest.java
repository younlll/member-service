package com.yeolcheong.mub.member.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Google Play RTDN(Pub/Sub push) 수신 본문. {@code message.data} 는 base64 인코딩된 알림 JSON 이다.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoogleNotificationRequest {

	private Message message;

	@Getter
	@NoArgsConstructor
	@AllArgsConstructor
	@Builder
	public static class Message {
		private String data;
		private String messageId;
	}
}
