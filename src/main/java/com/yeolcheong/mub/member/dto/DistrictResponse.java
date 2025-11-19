package com.yeolcheong.mub.member.dto;

import com.yeolcheong.mub.member.domain.District;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class DistrictResponse {

	@Getter
	@NoArgsConstructor
	@AllArgsConstructor
	@Builder
	public static class DistCode1 {
		private String code;
		private String name;
	}

	@Getter
	@NoArgsConstructor
	@AllArgsConstructor
	@Builder
	public static class DistCode2 {
		private String code;
		private String name;

		public static DistCode2 from(District district) {
			return DistCode2.builder()
				.code(district.getDistCode2())
				.name(district.getDistCode2Name())
				.build();
		}
	}
}
