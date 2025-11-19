package com.yeolcheong.mub.member.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/test")
@Slf4j
public class TestController {

	@GetMapping("/protected")
	public ResponseEntity<String> protectedEndpoint() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		Long memberId = (Long)authentication.getPrincipal();

		log.info("인증된 회원 ID: {}", memberId);

		return ResponseEntity.ok("인증 성공! 회원 ID: " + memberId);
	}
}
