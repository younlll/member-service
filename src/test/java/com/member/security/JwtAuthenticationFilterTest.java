package com.member.security;

import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.member.config.SecurityConfig;
import com.member.controller.TestController;

@WebMvcTest(TestController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
@DisplayName("JwtAuthenticationFilter 테스트")
class JwtAuthenticationFilterTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private JwtTokenProvider jwtTokenProvider;

	@Test
	@DisplayName("유효한 JWT 토큰으로 인증에 성공한다")
	void shouldAuthenticateWithValidToken() throws Exception {
		// given
		String token = "valid-jwt-token";
		Long memberId = 1L;

		given(jwtTokenProvider.validateToken(token)).willReturn(true);
		given(jwtTokenProvider.getMemberIdFromToken(token)).willReturn(memberId);

		// when & then
		mockMvc.perform(get("/api/test/protected").header("Authorization", "Bearer " + token))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(content().string("인증 성공! 회원 ID: " + memberId));
	}

	@Test
	@DisplayName("JWT 토큰이 없으면 403 에러를 반환한다")
	void shouldReturn403WithoutToken() throws Exception {
		// when & then
		mockMvc.perform(get("/api/test/protected")).andDo(print()).andExpect(status().isForbidden());
	}

	@Test
	@DisplayName("유효하지 않은 JWT 토큰으로 403 에러를 반환한다")
	void shouldReturn403WithInvalidToken() throws Exception {
		// given
		String invalidToken = "invalid-token";
		given(jwtTokenProvider.validateToken(invalidToken)).willReturn(false);

		// when & then
		mockMvc.perform(get("/api/test/protected").header("Authorization", "Bearer " + invalidToken))
			.andDo(print())
			.andExpect(status().isForbidden());
	}
}