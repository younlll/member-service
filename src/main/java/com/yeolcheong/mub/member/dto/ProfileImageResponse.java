package com.yeolcheong.mub.member.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 프로필 이미지 조회/변경 결과 응답.
 *
 * @see com.yeolcheong.mub.member.service.ProfileImageService
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfileImageResponse {

	// 공개 이미지 URL (base-url + 상대 경로 조합)
	private String imageUrl;

	// 기본 이미지 여부 (커스텀 이미지가 없어 기본 이미지로 응답한 경우 true)
	private boolean isDefault;

	public static ProfileImageResponse of(String imageUrl, boolean isDefault) {
		return ProfileImageResponse.builder()
			.imageUrl(imageUrl)
			.isDefault(isDefault)
			.build();
	}
}
