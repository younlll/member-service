package com.yeolcheong.mub.member.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.yeolcheong.mub.member.dto.InterestResponse;
import com.yeolcheong.mub.member.service.InterestService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/interests")
@RequiredArgsConstructor
@Slf4j
public class InterestController {

	private final InterestService interestService;

	/**
	 * 모든 관심사 목록과 관심사에 대한 옵션 목록 조회
	 * GET /api/interests
	 */
	@GetMapping
	public ResponseEntity<List<InterestResponse>> getAllInterests() {
		log.debug("Fetching interest list");

		List<InterestResponse> responses = interestService.getAllInterests();

		return ResponseEntity.ok(responses);
	}
}
