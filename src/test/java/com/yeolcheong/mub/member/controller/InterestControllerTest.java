package com.yeolcheong.mub.member.controller;

import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.yeolcheong.mub.member.domain.InterestOption;
import com.yeolcheong.mub.member.domain.InterestType;
import com.yeolcheong.mub.member.dto.InterestResponse;
import com.yeolcheong.mub.member.service.InterestService;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("InterestController 테스트")
class InterestControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private InterestService interestService;

	@Test
	@DisplayName("관심사 목록 조회 성공")
	void shouldGetInterests() throws Exception {
		// Given
		List<InterestResponse> interests = List.of(InterestResponse.builder()
			.interestType(InterestType.SELF_DEVELOPMENT)
			.description("자기계발")
			.availableOptions(List.of(InterestResponse.InterestOptionResponse.builder()
				.interestOption(InterestOption.INTERNET_AVAILABLE)
				.description("인터넷 가능")
				.build(), InterestResponse.InterestOptionResponse.builder()
				.interestOption(InterestOption.COMFORTABLE_CHAIR)
				.description("편한 의자")
				.build()))
			.build(), InterestResponse.builder()
			.interestType(InterestType.READING)
			.description("독서")
			.availableOptions(List.of(InterestResponse.InterestOptionResponse.builder()
				.interestOption(InterestOption.QUIET_MUSIC)
				.description("조용한 음악")
				.build(), InterestResponse.InterestOptionResponse.builder()
				.interestOption(InterestOption.WINDOW_SEAT)
				.description("창가 자리")
				.build()))
			.build());
		given(interestService.getAllInterests()).willReturn(interests);

		// When & Then
		mockMvc.perform(get("/api/interests"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].interestType").value("SELF_DEVELOPMENT"))
			.andExpect(jsonPath("$[0].description").value("자기계발"))
			.andExpect(jsonPath("$[0].availableOptions[0].interestOption").value("INTERNET_AVAILABLE"))
			.andExpect(jsonPath("$[0].availableOptions[0].description").value("인터넷 가능"))
			.andExpect(jsonPath("$[1].interestType").value("READING"))
			.andExpect(jsonPath("$[1].description").value("독서"));

		then(interestService).should().getAllInterests();
	}
}
