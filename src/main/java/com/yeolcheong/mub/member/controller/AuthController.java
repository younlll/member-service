package com.yeolcheong.mub.member.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.yeolcheong.mub.member.dto.KakaoTokenLoginRequest;
import com.yeolcheong.mub.member.dto.LoginRequest;
import com.yeolcheong.mub.member.dto.LoginResponse;
import com.yeolcheong.mub.member.dto.TokenRefreshRequest;
import com.yeolcheong.mub.member.dto.TokenRefreshResponse;
import com.yeolcheong.mub.member.service.AuthService;

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
		log.info("Login URL requested");

		String authUrl = authService.getAuthUrl();
		Map<String, String> response = new HashMap<>();
		response.put("authUrl", authUrl);

		return ResponseEntity.ok(response);
	}

	/**
	 * GET /api/auth/kakao/callback
	 */
	@GetMapping("/kakao/callback")
	public ResponseEntity<LoginResponse> kakaoCallback(@RequestParam String code) {
		log.info("Kakao callback received");

		LoginResponse loginResponse = authService.login(code);

		return ResponseEntity.ok(loginResponse);
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
		log.info("Kakao login API called");

		LoginResponse response = authService.login(request.getCode());

		return ResponseEntity.ok(response);
	}

	@PostMapping("/refresh")
	public ResponseEntity<TokenRefreshResponse> refreshToken(@Valid @RequestBody TokenRefreshRequest request) {
		log.info("Access token refresh requested");

		TokenRefreshResponse response = authService.refreshAccessToken(request.getRefreshToken());

		return ResponseEntity.ok(response);
	}

	@PostMapping("/login/token")
	public ResponseEntity<LoginResponse> loginWithKakaoToken(
		@Valid @RequestBody KakaoTokenLoginRequest request) {
		log.info("Login requested with Kakao SDK token");

		LoginResponse response = authService.loginWithKakaoToken(request.getAccessToken());
		return ResponseEntity.ok(response);
	}

	/**
	 * 로그아웃(본인). 저장된 리프레시 토큰을 무효화한다.
	 * POST /api/auth/logout
	 *
	 * @return 204 No Content
	 */
	@PostMapping("/logout")
	public ResponseEntity<Void> logout() {
		Long memberId = currentMemberId();
		log.info("Logout requested: memberId={}", memberId);

		authService.logout(memberId);
		return ResponseEntity.noContent().build();
	}

	private Long currentMemberId() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		return Long.valueOf(authentication.getName());
	}
}
