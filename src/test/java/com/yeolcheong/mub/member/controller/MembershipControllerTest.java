package com.yeolcheong.mub.member.controller;

import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.yeolcheong.mub.member.config.SecurityConfig;
import com.yeolcheong.mub.member.dto.MembershipProductResponse;
import com.yeolcheong.mub.member.repository.MemberRepository;
import com.yeolcheong.mub.member.security.JwtAuthenticationFilter;
import com.yeolcheong.mub.member.security.JwtTokenProvider;
import com.yeolcheong.mub.member.service.MembershipService;

@WebMvcTest(controllers = MembershipController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
@DisplayName("MembershipController slice tests")
class MembershipControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private MembershipService membershipService;

	@MockitoBean
	private JwtTokenProvider jwtTokenProvider;

	@MockitoBean
	private MemberRepository memberRepository;

	@Test
	@DisplayName("GET /api/memberships/product - returns 200 with product info when authenticated")
	@WithMockUser(username = "1")
	void getMembershipProductReturns200() throws Exception {
		given(membershipService.getActiveMembershipProduct()).willReturn(
			MembershipProductResponse.builder()
				.planName("머브크루").monthlyPrice(5000).benefits(List.of("공간 예약 할인"))
				.cohortNumber(3).recruitStartDate(LocalDate.now().minusDays(1))
				.recruitEndDate(LocalDate.now().plusDays(10)).recruiting(true).currentSubscribers(42L)
				.iosProductId("com.mub.app.membership.monthly").androidProductId("mub_membership_monthly")
				.build());

		mockMvc.perform(get("/api/memberships/product"))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.planName").value("머브크루"))
			.andExpect(jsonPath("$.monthlyPrice").value(5000))
			.andExpect(jsonPath("$.cohortNumber").value(3))
			.andExpect(jsonPath("$.recruiting").value(true))
			.andExpect(jsonPath("$.currentSubscribers").value(42))
			.andExpect(jsonPath("$.iosProductId").value("com.mub.app.membership.monthly"))
			.andExpect(jsonPath("$.androidProductId").value("mub_membership_monthly"));
	}

	@Test
	@DisplayName("GET /api/memberships/product - returns 4xx when unauthenticated")
	void getMembershipProductRequiresAuth() throws Exception {
		mockMvc.perform(get("/api/memberships/product"))
			.andDo(print())
			.andExpect(status().is4xxClientError());
	}
}
