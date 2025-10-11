package com.member.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.member.dto.KakaoUserInfoResponse;
import com.member.dto.LoginRequest;
import com.member.dto.LoginResponse;
import com.member.dto.LoginTokenResponse;
import com.member.service.AuthService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

	private final AuthService authService;

	/**
	 * 로그인 URL 조회
	 * GET /api/auth/login/url
	 *
	 * @return 로그인 URL
	 */
	@GetMapping("/login/url")
	public ResponseEntity<Map<String, String>> getAuthUrl() {
		log.info("로그인 URL 요청");

		String authUrl = authService.getAuthUrl();
		Map<String, String> response = new HashMap<>();
		response.put("authUrl", authUrl);

		return ResponseEntity.ok(response);
	}

	/**
	 * 로그인
	 * POST /api/auth/login
	 *
	 * @param request 카카오 인가 코드
	 * @return 인증 토큰 및 회원 정보
	 */
	@PostMapping("/login")
	public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
		log.info("카카오 로그인 API 호출: code={}", request.getCode());

		LoginResponse response = authService.login(request.getCode());

		return ResponseEntity.ok(response);
	}
}
