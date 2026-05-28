package com.yeolcheong.mub.member.controller;

import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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
import com.yeolcheong.mub.member.dto.LoginRequest;
import com.yeolcheong.mub.member.dto.LoginResponse;
import com.yeolcheong.mub.member.exception.GlobalExceptionHandler;
import com.yeolcheong.mub.member.exception.MemberServiceApiException;
import com.yeolcheong.mub.member.security.JwtAuthenticationFilter;
import com.yeolcheong.mub.member.security.JwtTokenProvider;
import com.yeolcheong.mub.member.service.AuthService;

@WebMvcTest(controllers = AuthController.class)
@Import({
	GlobalExceptionHandler.class,
	SecurityConfig.class,
	JwtAuthenticationFilter.class
})
@DisplayName("AuthController slice tests")
class AuthControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private AuthService authService;

	@MockitoBean
	private JwtTokenProvider jwtTokenProvider;

	@Test
	@DisplayName("GET /api/auth/login/url - returns auth URL")
	@WithMockUser
	void shouldReturnLoginUrl() throws Exception {
		// given
		String authUrl = "https://kauth.kakao.com/oauth/authorize?client_id=test&redirect_uri=test&response_type=code";
		given(authService.getAuthUrl()).willReturn(authUrl);

		// when & then
		mockMvc.perform(get("/api/auth/login/url"))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.authUrl").value(authUrl));
	}

	@Test
	@DisplayName("POST /api/auth/login - returns 200 and tokens when code is valid")
	@WithMockUser
	void shouldLoginSuccessfully() throws Exception {
		// given
		LoginRequest loginRequest = new LoginRequest("test-code");

		LoginResponse loginResponse = LoginResponse.builder()
			.tokenType("Bearer")
			.accessToken("jwt-access-token")
			.refreshToken("jwt-refresh-token")
			.expiresIn(86400L)
			.isNewMember(true)
			.kakaoId("1234567890")
			.email("test@example.com")
			.build();

		given(authService.login(anyString())).willReturn(loginResponse);

		// when & then
		mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON_VALUE)
				.content(objectMapper.writeValueAsString(loginRequest)))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.tokenType").value("Bearer"))
			.andExpect(jsonPath("$.accessToken").value("jwt-access-token"))
			.andExpect(jsonPath("$.refreshToken").value("jwt-refresh-token"))
			.andExpect(jsonPath("$.isNewMember").value(true))
			.andExpect(jsonPath("$.kakaoId").value("1234567890"))
			.andExpect(jsonPath("$.email").value("test@example.com"));
	}

	@Test
	@DisplayName("POST /api/auth/login - returns 400 when code is blank")
	@WithMockUser
	void shouldReturn400WhenCodeIsEmpty() throws Exception {
		// given
		LoginRequest loginRequest = new LoginRequest("");

		// when & given
		mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(loginRequest)))
			.andDo(print())
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("E40001"))
			.andExpect(jsonPath("$.message").value("입력값이 올바르지 않습니다"))
			.andExpect(jsonPath("$.errors").isArray())
			.andExpect(jsonPath("$.errors[0].field").value("code"))
			.andExpect(jsonPath("$.errors[0].reason").exists())
			.andExpect(jsonPath("$.timestamp").exists());
	}

	@Test
	@DisplayName("POST /api/auth/login - returns 500 when Kakao API call fails")
	@WithMockUser
	void shouldReturn500WhenKakaoApiFails() throws Exception {
		// given
		LoginRequest request = new LoginRequest("invalid-code");

		given(authService.login(anyString())).willThrow(new MemberServiceApiException("카카오 API 호출 실패"));

		// when & then
		mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andDo(print())
			.andExpect(status().isInternalServerError())
			.andExpect(jsonPath("$.code").value("E50002"))
			.andExpect(jsonPath("$.message").value("카카오 API 호출 실패"))
			.andExpect(jsonPath("$.timestamp").exists());
	}
}
