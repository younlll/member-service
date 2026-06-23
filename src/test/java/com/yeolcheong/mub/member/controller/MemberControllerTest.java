package com.yeolcheong.mub.member.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

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
import com.yeolcheong.mub.member.dto.ProfileResponse;
import com.yeolcheong.mub.member.dto.ProfileUpdateRequest;
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

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private MemberService memberService;

	@MockitoBean
	private JwtTokenProvider jwtTokenProvider;

	private ProfileUpdateRequest buildProfileRequest(String nickname) {
		InterestType type = InterestType.SELF_DEVELOPMENT;
		InterestOption option = type.getAvailableOptions().get(0);
		return ProfileUpdateRequest.builder()
			.nickname(nickname).bio("한줄소개").distCode1("11").distCode2("11680")
			.interests(List.of(new ProfileUpdateRequest.InterestRequest(type, List.of(option))))
			.build();
	}

	@Test
	@DisplayName("GET /api/members/me - returns 200 with profile")
	@WithMockUser(username = "1")
	void getMyProfileReturns200() throws Exception {
		given(memberService.getMyProfile(1L)).willReturn(
			ProfileResponse.builder().memberId(1L).nickname("테스터").bio("소개").build());

		mockMvc.perform(get("/api/members/me"))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.memberId").value(1))
			.andExpect(jsonPath("$.nickname").value("테스터"));
	}

	@Test
	@DisplayName("GET /api/members/me - returns 403 when unauthenticated")
	void getMyProfileReturns403WhenUnauthenticated() throws Exception {
		mockMvc.perform(get("/api/members/me"))
			.andDo(print())
			.andExpect(status().isForbidden());
	}

	@Test
	@DisplayName("PUT /api/members/me/profile - returns 200 on success")
	@WithMockUser(username = "1")
	void updateProfileReturns200() throws Exception {
		given(memberService.updateProfile(eq(1L), any(ProfileUpdateRequest.class)))
			.willReturn(ProfileResponse.builder().memberId(1L).nickname("새닉네임").build());

		mockMvc.perform(put("/api/members/me/profile")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(buildProfileRequest("새닉네임"))))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.nickname").value("새닉네임"));
	}

	@Test
	@DisplayName("PUT /api/members/me/profile - returns 400 when nickname exceeds max length")
	@WithMockUser(username = "1")
	void updateProfileReturns400WhenNicknameTooLong() throws Exception {
		mockMvc.perform(put("/api/members/me/profile")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(buildProfileRequest("12345678901"))))
			.andDo(print())
			.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("PUT /api/members/me/profile - returns 403 when unauthenticated")
	void updateProfileReturns403WhenUnauthenticated() throws Exception {
		mockMvc.perform(put("/api/members/me/profile")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(buildProfileRequest("닉네임"))))
			.andDo(print())
			.andExpect(status().isForbidden());
	}

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
