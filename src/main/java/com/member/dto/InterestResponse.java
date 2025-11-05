package com.member.dto;

import java.util.Arrays;
import java.util.List;

import com.member.domain.InterestOption;
import com.member.domain.InterestType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterestResponse {

	private InterestType interestType;
	private String description;
	private List<InterestOptionResponse> availableOptions;

	public static InterestResponse from(InterestType interestType) {
		return InterestResponse.builder()
			.interestType(interestType)
			.description(interestType.getDescription())
			.availableOptions(
				interestType.getAvailableOptions().stream()
					.map(InterestOptionResponse::from)
					.toList()
			)
			.build();
	}

	public static List<InterestResponse> getAllInterests() {
		return Arrays.stream(InterestType.values())
			.map(InterestResponse::from)
			.toList();
	}

	/**
	 * 관심사 옵션 응답
	 */
	@Getter
	@NoArgsConstructor
	@AllArgsConstructor
	@Builder
	public static class InterestOptionResponse {
		private InterestOption interestOption;
		private String description;

		public static InterestOptionResponse from(InterestOption option) {
			return InterestOptionResponse.builder()
				.interestOption(option)
				.description(option.getDescription())
				.build();
		}
	}
}
