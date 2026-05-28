package com.yeolcheong.mub.member.domain;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.yeolcheong.mub.member.domain.MemberStatus;
import com.yeolcheong.mub.member.domain.SnsProvider;

@DisplayName("MemberInterest")
class MemberInterestTest {

	@Test
	@DisplayName("addOption - stores options when added")
	void shouldAddOption() {
		// Given
		Member member = Member.builder()
			.snsProvider(SnsProvider.KAKAO)
			.socialId("12345")
			.status(MemberStatus.INACTIVE)
			.build();

		MemberInterest memberInterest = MemberInterest.builder()
			.member(member)
			.interestType(InterestType.SELF_DEVELOPMENT)
			.build();

		// When
		memberInterest.addOption(InterestOption.INTERNET_AVAILABLE);
		memberInterest.addOption(InterestOption.COMFORTABLE_CHAIR);

		// Then
		assertThat(memberInterest.getOptions()).hasSize(2);
		assertThat(memberInterest.getOptions())
			.extracting(MemberInterestOption::getOptionType)
			.containsExactlyInAnyOrder(
				InterestOption.INTERNET_AVAILABLE,
				InterestOption.COMFORTABLE_CHAIR
			);
	}

	@Test
	@DisplayName("addOption - ignores duplicate options")
	void shouldIgnoreDuplicateOption() {
		// Given
		Member member = Member.builder()
			.snsProvider(SnsProvider.KAKAO)
			.socialId("12345")
			.status(MemberStatus.INACTIVE)
			.build();

		MemberInterest memberInterest = MemberInterest.builder()
			.member(member)
			.interestType(InterestType.SELF_DEVELOPMENT)
			.build();

		// When
		memberInterest.addOption(InterestOption.INTERNET_AVAILABLE);
		memberInterest.addOption(InterestOption.INTERNET_AVAILABLE);

		// Then
		assertThat(memberInterest.getOptions()).hasSize(1);
		assertThat(memberInterest.getOptions().get(0).getOptionType())
			.isEqualTo(InterestOption.INTERNET_AVAILABLE);
	}

	@Test
	@DisplayName("addOption - starts with empty option list")
	void shouldStartWithEmptyOptions() {
		// Given
		Member member = Member.builder()
			.snsProvider(SnsProvider.KAKAO)
			.socialId("12345")
			.status(MemberStatus.INACTIVE)
			.build();

		// When
		MemberInterest memberInterest = MemberInterest.builder()
			.member(member)
			.interestType(InterestType.READING)
			.build();

		// Then
		assertThat(memberInterest.getOptions()).isEmpty();
	}

	@Test
	@DisplayName("addOption - stores all options when multiple options are added")
	void shouldAddMultipleOptions() {
		// Given
		Member member = Member.builder()
			.snsProvider(SnsProvider.KAKAO)
			.socialId("12345")
			.status(MemberStatus.INACTIVE)
			.build();

		MemberInterest memberInterest = MemberInterest.builder()
			.member(member)
			.interestType(InterestType.SPORTS)
			.build();

		// When
		memberInterest.addOption(InterestOption.OUTDOOR);
		memberInterest.addOption(InterestOption.INDOOR);
		memberInterest.addOption(InterestOption.SHOWER);

		// Then
		assertThat(memberInterest.getOptions()).hasSize(3);
	}
}
