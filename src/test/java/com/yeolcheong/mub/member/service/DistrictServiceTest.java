package com.yeolcheong.mub.member.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.yeolcheong.mub.member.domain.District;
import com.yeolcheong.mub.member.dto.DistrictResponse;
import com.yeolcheong.mub.member.repository.DistrictRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("DistrictService unit tests")
class DistrictServiceTest {

	@Mock
	private DistrictRepository districtRepository;

	@InjectMocks
	private DistrictService districtService;

	@Test
	@DisplayName("getDistCode1List - returns province list mapped from repository result")
	void shouldGetDistCode1ListSuccessfully() {
		// Given
		List<Object[]> mockData = Arrays.asList(
			new Object[] {"11", "서울특별시"},
			new Object[] {"26", "부산광역시"},
			new Object[] {"27", "대구광역시"}
		);
		given(districtRepository.findDistinctDistCode1()).willReturn(mockData);

		// When
		List<DistrictResponse.DistCode1> result = districtService.getDistCode1List();

		// Then
		assertThat(result).hasSize(3);
		assertThat(result.get(0).getCode()).isEqualTo("11");
		assertThat(result.get(0).getName()).isEqualTo("서울특별시");
		assertThat(result.get(1).getCode()).isEqualTo("26");
		assertThat(result.get(1).getName()).isEqualTo("부산광역시");

		then(districtRepository).should().findDistinctDistCode1();
	}

	@Test
	@DisplayName("getDistCode2List - returns district list for given province code")
	void shouldGetDistCode2ListSuccessfully() {
		// Given
		String distCode1 = "11";
		List<District> mockDistricts = Arrays.asList(
			District.builder()
				.distCode1("11")
				.distCode1Name("서울특별시")
				.distCode2("11680")
				.distCode2Name("강남구")
				.build(),
			District.builder()
				.distCode1("11")
				.distCode1Name("서울특별시")
				.distCode2("11740")
				.distCode2Name("강동구")
				.build()
		);
		given(districtRepository.findByDistCode1(distCode1)).willReturn(mockDistricts);

		// When
		List<DistrictResponse.DistCode2> result = districtService.getDistCode2List(distCode1);

		// Then
		assertThat(result).hasSize(2);
		assertThat(result.get(0).getCode()).isEqualTo("11680");
		assertThat(result.get(0).getName()).isEqualTo("강남구");
		assertThat(result.get(1).getCode()).isEqualTo("11740");
		assertThat(result.get(1).getName()).isEqualTo("강동구");

		then(districtRepository).should().findByDistCode1(distCode1);
	}

	@Test
	@DisplayName("getDistCode2List - returns empty list when no districts match the province code")
	void shouldReturnEmptyListWhenNoDistricts() {
		// Given
		String distCode1 = "99";
		given(districtRepository.findByDistCode1(distCode1)).willReturn(List.of());

		// When
		List<DistrictResponse.DistCode2> result = districtService.getDistCode2List(distCode1);

		// Then
		assertThat(result).isEmpty();
		then(districtRepository).should().findByDistCode1(distCode1);
	}
}
