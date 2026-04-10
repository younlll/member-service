package com.yeolcheong.mub.member.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
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
		log.info("로그인 URL 요청");

		String authUrl = authService.getAuthUrl();
		Map<String, String> response = new HashMap<>();
		response.put("authUrl", authUrl);

		return ResponseEntity.ok(response);
	}

	/**
	 * GET /api/auth/kakao/callback
	 */
	@GetMapping("/kakao/callback")
	public ResponseEntity<?> kakaoCallback(@RequestParam String code) {
		log.info("카카오 콜백 수신: code={}", code);

		LoginResponse loginResponse = authService.login(code);

		return ResponseEntity.ok(loginResponse);

		// try {
		// 	LoginResponse loginResponse = authService.login(code);
		//
		// 	String frontendUrl = String.format(
		// 		"http://localhost:3000/auth/callback?accessToken=%s&refreshToken=%s&isNewMember=%s&memberId=%s",
		// 		loginResponse.getAccessToken(),
		// 		loginResponse.getRefreshToken(),
		// 		loginResponse.getIsNewMember(),
		// 		loginResponse.getMemberId()
		// 	);
		//
		// 	return ResponseEntity.status(HttpStatus.FOUND)
		// 		.header("Location", frontendUrl)
		// 		.build();
		//
		// } catch (Exception e) {
		// 	log.error("카카오 로그인 콜백 처리 실패", e);
		//
		// 	String errorUrl = "http://localhost:3000/auth/error?message=" +
		// 		java.net.URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8);
		//
		// 	return ResponseEntity.status(HttpStatus.FOUND)
		// 		.header("Location", errorUrl)
		// 		.build();
		// }
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

	@PostMapping("/refresh")
	public ResponseEntity<TokenRefreshResponse> refreshToken(@Valid @RequestBody TokenRefreshRequest request) {
		log.info("Access Token 재발급 요청");

		TokenRefreshResponse response = authService.refreshAccessToken(request.getRefreshToken());

		return ResponseEntity.ok(response);
	}

	@PostMapping("/login/token")
	public ResponseEntity<LoginResponse> loginWithKakaoToken(
		@RequestBody KakaoTokenLoginRequest request) {
		log.info("카카오 SDK 토큰으로 로그인 요청");

		LoginResponse response = authService.loginWithKakaoToken(request.getAccessToken());
		return ResponseEntity.ok(response);
	}
}
