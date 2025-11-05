package com.member.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.member.dto.OnboardingRequest;
import com.member.dto.OnboardingResponse;
import com.member.service.OnboardingService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/onboarding")
@RequiredArgsConstructor
@Slf4j
public class OnboardingController {

	private final OnboardingService onboardingService;

	/**
	 * 회원가입 완료
	 * POST /api/onboarding/complete
	 */
	@PostMapping("/complete")
	public ResponseEntity<OnboardingResponse> completeOnboarding(@Valid @RequestBody OnboardingRequest request) {
		log.info("회원가입 완료 요청: nickname={}", request.getNickname());

		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		Long memberId = Long.valueOf(authentication.getName());

		OnboardingResponse onboardingResponse = onboardingService.completeOnboarding(memberId, request);

		return ResponseEntity.ok(onboardingResponse);
	}
}
