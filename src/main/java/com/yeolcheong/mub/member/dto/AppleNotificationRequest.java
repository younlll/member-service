package com.yeolcheong.mub.member.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Apple App Store Server Notifications V2 수신 본문. 서명된 페이로드(JWS)만 담긴다.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppleNotificationRequest {

	private String signedPayload;
}
