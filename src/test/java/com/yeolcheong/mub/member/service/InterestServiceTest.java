package com.yeolcheong.mub.member.service;

import static org.assertj.core.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.yeolcheong.mub.member.domain.InterestType;
import com.yeolcheong.mub.member.dto.InterestResponse;

@ExtendWith(MockitoExtension.class)
@DisplayName("InterestService unit tests")
class InterestServiceTest {

	@InjectMocks
	private InterestService interestService;

	@Test
	@DisplayName("getAllInterests - returns one response per InterestType with description and options")
	void shouldGetAllInterestsSuccessfully() {
		// When
		List<InterestResponse> result = interestService.getAllInterests();

		// Then
		assertThat(result).hasSize(InterestType.values().length);
		assertThat(result).extracting("interestType")
			.contains(InterestType.SELF_DEVELOPMENT, InterestType.INVESTMENT, InterestType.READING,
				InterestType.EDUCATION, InterestType.SPORTS, InterestType.RESTAURANT, InterestType.MUSIC,
				InterestType.DANCE, InterestType.PHOTO);

		// 각 관심사는 옵션을 가져야 함
		result.forEach(interest -> {
			assertThat(interest.getDescription()).isNotNull();
			assertThat(interest.getAvailableOptions()).isNotEmpty();
		});
	}

	@Test
	@DisplayName("getAllInterests - SELF_DEVELOPMENT exposes expected description and options")
	void shouldHaveCorrectOptionsForSelfDevelopment() {
		// When
		List<InterestResponse> result = interestService.getAllInterests();
		InterestResponse selfDevelopment = result.stream()
			.filter(interest -> interest.getInterestType() == InterestType.SELF_DEVELOPMENT)
			.findFirst()
			.orElseThrow();

		// Then
		assertThat(selfDevelopment.getDescription()).isEqualTo("자기계발");
		assertThat(selfDevelopment.getAvailableOptions()).hasSizeGreaterThan(10);
	}

	@Test
	@DisplayName("getAllInterests - SPORTS exposes expected description and options")
	void shouldHaveCorrectOptionsForSports() {
		// When
		List<InterestResponse> result = interestService.getAllInterests();
		InterestResponse sports = result.stream()
			.filter(interest -> interest.getInterestType() == InterestType.SPORTS)
			.findFirst()
			.orElseThrow();

		// Then
		assertThat(sports.getDescription()).isEqualTo("운동/스포츠");
		assertThat(sports.getAvailableOptions()).extracting("description").contains("잔디구장", "주차", "샤워", "장비대여");
	}
}
