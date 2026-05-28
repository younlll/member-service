package com.yeolcheong.mub.member.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.yeolcheong.mub.member.dto.DistrictResponse;
import com.yeolcheong.mub.member.service.DistrictService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/districts")
@RequiredArgsConstructor
@Slf4j
public class DistrictController {

	private final DistrictService districtService;

	/**
	 * 모든 시/도 목록 조회
	 * GET /api/districts/code1
	 */
	@GetMapping("/code1")
	public ResponseEntity<List<DistrictResponse.DistCode1>> getDistCode1List() {
		log.debug("Fetching province list");

		List<DistrictResponse.DistCode1> response = districtService.getDistCode1List();

		return ResponseEntity.ok(response);
	}

	/**
	 * 특정 시/도의  시/군/구 목록 조회
	 * GET /api/districts/code2?distCode1={distCode1}
	 */
	@GetMapping("/code2")
	public ResponseEntity<List<DistrictResponse.DistCode2>> getDistCode2List(@RequestParam String distCode1) {
		log.debug("Fetching district list: distCode1={}", distCode1);

		List<DistrictResponse.DistCode2> responses = districtService.getDistCode2List(distCode1);
		return ResponseEntity.ok(responses);
	}
}
