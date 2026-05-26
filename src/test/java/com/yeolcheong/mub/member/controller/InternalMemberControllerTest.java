package com.yeolcheong.mub.member.controller;

import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.yeolcheong.mub.member.dto.MemberSummaryResponse;
import com.yeolcheong.mub.member.service.MemberService;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("InternalMemberController")
class InternalMemberControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private MemberService memberService;

	@Test
	@DisplayName("should return summaries for the requested ids without requiring auth")
	void shouldReturnSummariesForRequestedIds() throws Exception {
		// given
		List<MemberSummaryResponse> summaries = List.of(
			MemberSummaryResponse.builder()
				.memberId(1L)
				.nickname("테스터")
				.email("test@example.com")
				.build(),
			MemberSummaryResponse.builder()
				.memberId(2L)
				.nickname("아더")
				.email("other@example.com")
				.build()
		);
		given(memberService.findSummariesByIds(List.of(1L, 2L))).willReturn(summaries);

		// when & then
		mockMvc.perform(get("/api/internal/members").param("ids", "1", "2"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(2))
			.andExpect(jsonPath("$[0].memberId").value(1))
			.andExpect(jsonPath("$[0].nickname").value("테스터"))
			.andExpect(jsonPath("$[0].email").value("test@example.com"))
			.andExpect(jsonPath("$[1].memberId").value(2));

		then(memberService).should().findSummariesByIds(List.of(1L, 2L));
	}

	@Test
	@DisplayName("should return empty array when no members match the requested ids")
	void shouldReturnEmptyArrayWhenNoMatches() throws Exception {
		// given
		given(memberService.findSummariesByIds(List.of(99L))).willReturn(Collections.emptyList());

		// when & then
		mockMvc.perform(get("/api/internal/members").param("ids", "99"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(0));
	}

	@Test
	@DisplayName("should accept comma-separated ids in a single query param")
	void shouldAcceptCommaSeparatedIds() throws Exception {
		// given
		given(memberService.findSummariesByIds(List.of(1L, 2L, 3L)))
			.willReturn(Collections.emptyList());

		// when & then — Spring binds "1,2,3" into List<Long> when one ids param given
		mockMvc.perform(get("/api/internal/members").param("ids", "1,2,3"))
			.andExpect(status().isOk());

		then(memberService).should().findSummariesByIds(List.of(1L, 2L, 3L));
	}
}
