package com.yeolcheong.mub.member.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.yeolcheong.mub.member.dto.OnboardingRequest;
import com.yeolcheong.mub.member.dto.OnboardingResponse;
import com.yeolcheong.mub.member.service.OnboardingService;

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
	 * 회원가입 완료.
	 * <p>
	 * multipart/form-data 로 온보딩 입력(JSON {@code request} 파트)과 프로필 이미지 파일(선택,
	 * {@code profileImage} 파트)을 함께 받는다. 이미지가 있으면 스토리지에 저장 후 회원에 연결한다.
	 * POST /api/onboarding/complete
	 */
	@PostMapping(value = "/complete", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<OnboardingResponse> completeOnboarding(
		@Valid @RequestPart("request") OnboardingRequest request,
		@RequestPart(value = "profileImage", required = false) MultipartFile profileImage) {
		log.info("Onboarding complete requested: nickname={}, hasImage={}",
			request.getNickname(), profileImage != null && !profileImage.isEmpty());

		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		Long memberId = Long.valueOf(authentication.getName());

		OnboardingResponse onboardingResponse = onboardingService.completeOnboarding(memberId, request, profileImage);

		return ResponseEntity.status(HttpStatus.CREATED).body(onboardingResponse);
	}
}
