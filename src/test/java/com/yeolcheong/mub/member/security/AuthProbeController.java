package com.yeolcheong.mub.member.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Test-only probe controller used by JwtAuthenticationFilterTest to exercise
 * the JWT authentication flow against a real secured endpoint. Lives under
 * src/test so it is never registered in production.
 */
@RestController
@RequestMapping("/api/test")
class AuthProbeController {

	@GetMapping("/protected")
	String protectedEndpoint() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		Long memberId = (Long)authentication.getPrincipal();
		return "authenticated:" + memberId;
	}
}
