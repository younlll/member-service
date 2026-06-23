package com.yeolcheong.mub.member.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.yeolcheong.mub.member.config.SecurityConfig;
import com.yeolcheong.mub.member.exception.ErrorCode;
import com.yeolcheong.mub.member.exception.MemberServiceApiException;
import com.yeolcheong.mub.member.security.JwtAuthenticationFilter;
import com.yeolcheong.mub.member.security.JwtTokenProvider;
import com.yeolcheong.mub.member.service.MemberService;

@WebMvcTest(controllers = MemberController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
@DisplayName("MemberController slice tests")
class MemberControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private MemberService memberService;

	@MockitoBean
	private JwtTokenProvider jwtTokenProvider;

	@Test
	@DisplayName("DELETE /api/members/me - returns 204 on successful withdrawal")
	@WithMockUser(username = "1")
	void withdrawReturns204() throws Exception {
		willDoNothing().given(memberService).withdraw(1L);

		mockMvc.perform(delete("/api/members/me"))
			.andDo(print())
			.andExpect(status().isNoContent());

		then(memberService).should().withdraw(1L);
	}

	@Test
	@DisplayName("DELETE /api/members/me - returns 403 when unauthenticated")
	void withdrawReturns403WhenUnauthenticated() throws Exception {
		mockMvc.perform(delete("/api/members/me"))
			.andDo(print())
			.andExpect(status().isForbidden());

		then(memberService).should(never()).withdraw(anyLong());
	}

	@Test
	@DisplayName("DELETE /api/members/me - returns 400 when member already withdrawn")
	@WithMockUser(username = "1")
	void withdrawReturns400WhenAlreadyWithdrawn() throws Exception {
		willThrow(new MemberServiceApiException(ErrorCode.ALREADY_WITHDRAWN))
			.given(memberService).withdraw(1L);

		mockMvc.perform(delete("/api/members/me"))
			.andDo(print())
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("E40012"));
	}

	@Test
	@DisplayName("DELETE /api/members/me - returns 404 when member does not exist")
	@WithMockUser(username = "999")
	void withdrawReturns404WhenMemberNotFound() throws Exception {
		willThrow(new MemberServiceApiException(ErrorCode.MEMBER_NOT_FOUND))
			.given(memberService).withdraw(999L);

		mockMvc.perform(delete("/api/members/me"))
			.andDo(print())
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("E40401"));
	}
}
