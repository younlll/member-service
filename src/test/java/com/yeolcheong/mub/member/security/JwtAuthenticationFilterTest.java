package com.yeolcheong.mub.member.security;

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

import com.yeolcheong.mub.member.config.SecurityConfig;
import com.yeolcheong.mub.member.domain.MemberStatus;
import com.yeolcheong.mub.member.repository.MemberRepository;

@WebMvcTest(AuthProbeController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
@DisplayName("JwtAuthenticationFilter slice tests")
class JwtAuthenticationFilterTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private JwtTokenProvider jwtTokenProvider;

	@MockitoBean
	private MemberRepository memberRepository;

	@Test
	@DisplayName("should authenticate request when JWT token is valid and member is active")
	void shouldAuthenticateWithValidToken() throws Exception {
		// given
		String token = "valid-jwt-token";
		Long memberId = 1L;

		given(jwtTokenProvider.validateToken(token)).willReturn(true);
		given(jwtTokenProvider.getMemberIdFromToken(token)).willReturn(memberId);
		given(memberRepository.existsByIdAndStatusNot(memberId, MemberStatus.DELETED)).willReturn(true);

		// when & then
		mockMvc.perform(get("/api/test/protected").header("Authorization", "Bearer " + token))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(content().string("authenticated:" + memberId));
	}

	@Test
	@DisplayName("should return 403 when token is valid but member is withdrawn (DELETED)")
	void shouldReturn403WhenMemberWithdrawn() throws Exception {
		// given
		String token = "valid-jwt-token";
		Long memberId = 1L;

		given(jwtTokenProvider.validateToken(token)).willReturn(true);
		given(jwtTokenProvider.getMemberIdFromToken(token)).willReturn(memberId);
		given(memberRepository.existsByIdAndStatusNot(memberId, MemberStatus.DELETED)).willReturn(false);

		// when & then
		mockMvc.perform(get("/api/test/protected").header("Authorization", "Bearer " + token))
			.andDo(print())
			.andExpect(status().isForbidden());
	}

	@Test
	@DisplayName("should return 403 when JWT token is missing")
	void shouldReturn403WithoutToken() throws Exception {
		// when & then
		mockMvc.perform(get("/api/test/protected")).andDo(print()).andExpect(status().isForbidden());
	}

	@Test
	@DisplayName("should return 403 when JWT token is invalid")
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
