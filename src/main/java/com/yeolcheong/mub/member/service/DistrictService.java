package com.yeolcheong.mub.member.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yeolcheong.mub.member.domain.District;
import com.yeolcheong.mub.member.dto.DistrictResponse;
import com.yeolcheong.mub.member.repository.DistrictRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class DistrictService {

	private final DistrictRepository districtRepository;

	public List<DistrictResponse.DistCode1> getDistCode1List() {
		log.debug("시/도 목록 조회");

		List<Object[]> results = districtRepository.findDistinctDistCode1();

		return results.stream()
			.map(result -> DistrictResponse.DistCode1.builder()
				.code((String)result[0])
				.name((String)result[1])
				.build())
			.toList();
	}

	public List<DistrictResponse.DistCode2> getDistCode2List(String distCode1) {
		log.debug("구/시 목록 조회: distCode1={}", distCode1);

		List<District> districts = districtRepository.findByDistCode1(distCode1);

		return districts.stream()
			.map(DistrictResponse.DistCode2::from)
			.toList();
	}
}
