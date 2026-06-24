package com.yeolcheong.mub.member.controller;

import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Arrays;
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
import com.yeolcheong.mub.member.dto.DistrictResponse;
import com.yeolcheong.mub.member.security.JwtAuthenticationFilter;
import com.yeolcheong.mub.member.security.JwtTokenProvider;
import com.yeolcheong.mub.member.repository.MemberRepository;
import com.yeolcheong.mub.member.service.DistrictService;

@WebMvcTest(controllers = DistrictController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
@DisplayName("DistrictController slice tests")
class DistrictControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private DistrictService districtService;

	@MockitoBean
	private JwtTokenProvider jwtTokenProvider;

	@MockitoBean
	private MemberRepository memberRepository;

	@Test
	@DisplayName("GET /api/districts/code1 - returns province list")
	@WithMockUser
	void shouldGetDistCode1List() throws Exception {
		// given
		List<DistrictResponse.DistCode1> distCode1List = Arrays.asList(
			new DistrictResponse.DistCode1("11", "서울특별시"),
			new DistrictResponse.DistCode1("26", "부산광역시")
		);
		given(districtService.getDistCode1List()).willReturn(distCode1List);

		// when & then
		mockMvc.perform(get("/api/districts/code1"))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].code").value("11"))
			.andExpect(jsonPath("$[0].name").value("서울특별시"))
			.andExpect(jsonPath("$[1].code").value("26"))
			.andExpect(jsonPath("$[1].name").value("부산광역시"));

		then(districtService).should().getDistCode1List();
	}

	@Test
	@DisplayName("GET /api/districts/code2 - returns districts for given province")
	@WithMockUser
	void shouldGetDistCode2List() throws Exception {
		// given
		List<DistrictResponse.DistCode2> distCode2List = Arrays.asList(
			new DistrictResponse.DistCode2("11680", "강남구"),
			new DistrictResponse.DistCode2("11740", "강동구")
		);
		given(districtService.getDistCode2List("11")).willReturn(distCode2List);

		// when & then
		mockMvc.perform(get("/api/districts/code2")
				.param("distCode1", "11"))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].code").value("11680"))
			.andExpect(jsonPath("$[0].name").value("강남구"))
			.andExpect(jsonPath("$[1].code").value("11740"))
			.andExpect(jsonPath("$[1].name").value("강동구"));

		then(districtService).should().getDistCode2List("11");
	}
}
