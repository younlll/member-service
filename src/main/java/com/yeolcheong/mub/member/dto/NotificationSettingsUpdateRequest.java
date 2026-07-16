package com.yeolcheong.mub.member.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 알림 설정 수정 요청.
 * 광고성(마케팅) 수신 동의 여부를 토글한다.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationSettingsUpdateRequest {

	@NotNull(message = "마케팅 수신 동의 여부는 필수입니다")
	private Boolean marketingAgreed;
}
