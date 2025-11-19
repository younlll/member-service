package com.yeolcheong.mub.member.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yeolcheong.mub.member.domain.InterestOption;
import com.yeolcheong.mub.member.domain.InterestType;
import com.yeolcheong.mub.member.dto.OnboardingRequest;
import com.yeolcheong.mub.member.dto.OnboardingResponse;
import com.yeolcheong.mub.member.exception.ErrorCode;
import com.yeolcheong.mub.member.exception.MemberServiceApiException;
import com.yeolcheong.mub.member.service.OnboardingService;

import jakarta.servlet.ServletException;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("OnboardingController 테스트")
class OnboardingControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private OnboardingService onboardingService;

	private OnboardingRequest validRequest;

	@BeforeEach
	void setUp() throws ServletException, IOException {
		validRequest = OnboardingRequest.builder()
			.termsAgreementRequest(new OnboardingRequest.TermsAgreementRequest(true, true, true, false))
			.nickname("테스트유저")
			.distCode1("11")
			.distCode2("11680")
			.interests(List.of(new OnboardingRequest.InterestRequest(InterestType.SELF_DEVELOPMENT,
				Arrays.asList(InterestOption.INTERNET_AVAILABLE, InterestOption.COMFORTABLE_CHAIR))))
			.build();
	}

	@Test
	@DisplayName("회원가입에 성공한다")
	@WithMockUser(username = "1")
	void shouldCompleteOnboardingSuccessfully() throws Exception {
		// Given
		OnboardingResponse response = OnboardingResponse.of(1L, "테스트유저");
		given(onboardingService.completeOnboarding(eq(1L), any(OnboardingRequest.class))).willReturn(response);

		// When & Then
		mockMvc.perform(post("/api/onboarding/complete").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(validRequest)))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.memberId").value(1))
			.andExpect(jsonPath("$.nickname").value("테스트유저"))
			.andExpect(jsonPath("$.message").value("회원가입이 완료되었습니다"));

		then(onboardingService).should().completeOnboarding(eq(1L), any(OnboardingRequest.class));
	}

	@Test
	@DisplayName("인증되지 않은 사용자의 회원가입은 실패한다")
	void shouldFailWhenNotAuthenticated() throws Exception {
		mockMvc.perform(post("/api/onboarding/complete").contentType(MediaType.APPLICATION_JSON)
			.content(objectMapper.writeValueAsString(validRequest))).andExpect(status().isForbidden());

		then(onboardingService).should(never()).completeOnboarding(anyLong(), any());
	}

	@Test
	@DisplayName("닉네임의 길이가 초과되어 회원가입에 실패한다")
	@WithMockUser(username = "1")
	void shouldFailWhenNicknameTooLong() throws Exception {
		OnboardingRequest invalidRequest = OnboardingRequest.builder()
			.termsAgreementRequest(validRequest.getTermsAgreementRequest())
			.nickname("12345678901")
			.distCode1("11")
			.distCode2("11680")
			.interests(validRequest.getInterests())
			.build();

		mockMvc.perform(post("/api/onboarding/complete").contentType(MediaType.APPLICATION_JSON)
			.content(objectMapper.writeValueAsString(invalidRequest))).andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("유효하지 않은 지역코드의 요청은 회원가입에 실패한다")
	@WithMockUser(username = "1")
	void shouldFailWhenInvalidDistrict() throws Exception {
		OnboardingRequest invalidRequest = OnboardingRequest.builder()
			.termsAgreementRequest(validRequest.getTermsAgreementRequest())
			.nickname("12345678901")  // 11자 - @Size(max = 10) 위반
			.distCode1("11")
			.distCode2("11680")
			.interests(validRequest.getInterests())
			.build();

		// When & Then
		mockMvc.perform(post("/api/onboarding/complete").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(invalidRequest)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("E40001"))
			.andExpect(jsonPath("$.message").value("입력값이 올바르지 않습니다"));

		then(onboardingService).should(never()).completeOnboarding(anyLong(), any());
	}

	@Test
	@DisplayName("로그인 회원이 아닌경우 회원가입에 실패한다")
	@WithMockUser(username = "999")
	void shouldFailWhenMemberNotFound() throws Exception {
		// Given - Mock 설정 필수!
		given(onboardingService.completeOnboarding(eq(999L), any())).willThrow(
			new MemberServiceApiException(ErrorCode.MEMBER_NOT_FOUND));

		// When & Then
		mockMvc.perform(post("/api/onboarding/complete").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(validRequest)))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("E40401"));

		// Service 호출 검증
		then(onboardingService).should().completeOnboarding(eq(999L), any());
	}
}
