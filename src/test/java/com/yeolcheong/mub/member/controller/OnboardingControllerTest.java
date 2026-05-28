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
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yeolcheong.mub.member.config.SecurityConfig;
import com.yeolcheong.mub.member.domain.InterestOption;
import com.yeolcheong.mub.member.domain.InterestType;
import com.yeolcheong.mub.member.dto.OnboardingRequest;
import com.yeolcheong.mub.member.dto.OnboardingResponse;
import com.yeolcheong.mub.member.exception.ErrorCode;
import com.yeolcheong.mub.member.exception.MemberServiceApiException;
import com.yeolcheong.mub.member.exception.OnboardingServiceApiException;
import com.yeolcheong.mub.member.security.JwtAuthenticationFilter;
import com.yeolcheong.mub.member.security.JwtTokenProvider;
import com.yeolcheong.mub.member.service.OnboardingService;

import jakarta.servlet.ServletException;

@WebMvcTest(controllers = OnboardingController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
@DisplayName("OnboardingController slice tests")
class OnboardingControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private OnboardingService onboardingService;

	@MockitoBean
	private JwtTokenProvider jwtTokenProvider;

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
	@DisplayName("POST /api/onboarding/complete - returns 201 when onboarding succeeds")
	@WithMockUser(username = "1")
	void shouldCompleteOnboardingSuccessfully() throws Exception {
		// given
		OnboardingResponse response = OnboardingResponse.of(1L, "테스트유저");
		given(onboardingService.completeOnboarding(eq(1L), any(OnboardingRequest.class))).willReturn(response);

		// when & then
		mockMvc.perform(post("/api/onboarding/complete").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(validRequest)))
			.andDo(print())
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.memberId").value(1))
			.andExpect(jsonPath("$.nickname").value("테스트유저"))
			.andExpect(jsonPath("$.message").value("회원가입이 완료되었습니다"));

		then(onboardingService).should().completeOnboarding(eq(1L), any(OnboardingRequest.class));
	}

	@Test
	@DisplayName("POST /api/onboarding/complete - returns 403 when caller is unauthenticated")
	void shouldFailWhenNotAuthenticated() throws Exception {
		mockMvc.perform(post("/api/onboarding/complete").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(validRequest)))
			.andDo(print())
			.andExpect(status().isForbidden());

		then(onboardingService).should(never()).completeOnboarding(anyLong(), any());
	}

	@Test
	@DisplayName("POST /api/onboarding/complete - returns 400 when nickname exceeds max length")
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
				.content(objectMapper.writeValueAsString(invalidRequest)))
			.andDo(print())
			.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("POST /api/onboarding/complete - returns 400 with INVALID_DISTRICT_CODE when service rejects the district")
	@WithMockUser(username = "1")
	void shouldFailWhenInvalidDistrictCode() throws Exception {
		// given — 닉네임 등 모든 입력은 유효, 서비스 단에서 지역 코드 검증 실패
		given(onboardingService.completeOnboarding(eq(1L), any(OnboardingRequest.class)))
			.willThrow(new OnboardingServiceApiException(ErrorCode.INVALID_DISTRICT_CODE));

		// when & then
		mockMvc.perform(post("/api/onboarding/complete").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(validRequest)))
			.andDo(print())
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("E40010"))
			.andExpect(jsonPath("$.message").value("유효하지 않은 지역 코드입니다"));

		then(onboardingService).should().completeOnboarding(eq(1L), any(OnboardingRequest.class));
	}

	@Test
	@DisplayName("POST /api/onboarding/complete - returns 404 when authenticated member does not exist")
	@WithMockUser(username = "999")
	void shouldFailWhenMemberNotFound() throws Exception {
		// given - Mock 설정 필수!
		given(onboardingService.completeOnboarding(eq(999L), any())).willThrow(
			new MemberServiceApiException(ErrorCode.MEMBER_NOT_FOUND));

		// when & then
		mockMvc.perform(post("/api/onboarding/complete").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(validRequest)))
			.andDo(print())
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("E40401"));

		// Service 호출 검증
		then(onboardingService).should().completeOnboarding(eq(999L), any());
	}
}
