package com.member.controller;

import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.member.dto.DistrictResponse;
import com.member.service.DistrictService;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("DistrictController 테스트")
class DistrictControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private DistrictService districtService;

	@Test
	@DisplayName("시/도 목록 조회 성공")
	void shouldGetDistCode1List() throws Exception {
		// Given
		List<DistrictResponse.DistCode1> distCode1List = Arrays.asList(
			new DistrictResponse.DistCode1("11", "서울특별시"),
			new DistrictResponse.DistCode1("26", "부산광역시")
		);
		given(districtService.getDistCode1List()).willReturn(distCode1List);

		// When & Then
		mockMvc.perform(get("/api/districts/code1"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].code").value("11"))
			.andExpect(jsonPath("$[0].name").value("서울특별시"))
			.andExpect(jsonPath("$[1].code").value("26"))
			.andExpect(jsonPath("$[1].name").value("부산광역시"));

		then(districtService).should().getDistCode1List();
	}

	@Test
	@DisplayName("구/시 목록 조회 성공")
	void shouldGetDistCode2List() throws Exception {
		// Given
		List<DistrictResponse.DistCode2> distCode2List = Arrays.asList(
			new DistrictResponse.DistCode2("11680", "강남구"),
			new DistrictResponse.DistCode2("11740", "강동구")
		);
		given(districtService.getDistCode2List("11")).willReturn(distCode2List);

		// When & Then
		mockMvc.perform(get("/api/districts/code2")
				.param("distCode1", "11"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].code").value("11680"))
			.andExpect(jsonPath("$[0].name").value("강남구"))
			.andExpect(jsonPath("$[1].code").value("11740"))
			.andExpect(jsonPath("$[1].name").value("강동구"));

		then(districtService).should().getDistCode2List("11");
	}
}
