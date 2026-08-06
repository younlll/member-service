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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.yeolcheong.mub.member.config.SecurityConfig;
import com.yeolcheong.mub.member.repository.MemberRepository;
import com.yeolcheong.mub.member.security.JwtAuthenticationFilter;
import com.yeolcheong.mub.member.security.JwtTokenProvider;
import com.yeolcheong.mub.member.service.MembershipWebhookService;

@WebMvcTest(controllers = MembershipWebhookController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
@DisplayName("MembershipWebhookController slice tests")
class MembershipWebhookControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private MembershipWebhookService webhookService;

	@MockitoBean
	private JwtTokenProvider jwtTokenProvider;

	@MockitoBean
	private MemberRepository memberRepository;

	@Test
	@DisplayName("POST /webhook/apple - returns 200 and delegates without authentication")
	void appleWebhookReturns200() throws Exception {
		mockMvc.perform(post("/api/memberships/webhook/apple")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"signedPayload\":\"payload\"}"))
			.andDo(print())
			.andExpect(status().isOk());

		then(webhookService).should().handleAppleNotification("payload");
	}

	@Test
	@DisplayName("POST /webhook/google - returns 200 and delegates the base64 data without authentication")
	void googleWebhookReturns200() throws Exception {
		mockMvc.perform(post("/api/memberships/webhook/google")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"message\":{\"data\":\"encoded\",\"messageId\":\"m1\"}}"))
			.andDo(print())
			.andExpect(status().isOk());

		then(webhookService).should().handleGoogleNotification("encoded");
	}
}
