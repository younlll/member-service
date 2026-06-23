package com.yeolcheong.mub.member.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.yeolcheong.mub.member.dto.MemberInfoResponse;
import com.yeolcheong.mub.member.service.MemberService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
@Slf4j
public class MemberController {

	private final MemberService memberService;

	/**
	 * 이메일로 회원 정보 조회
	 * GET /api/members?email=user@kakao.com
	 */
	@GetMapping
	public ResponseEntity<MemberInfoResponse> getMemberByEmail(
		@RequestParam String email) {
		log.info("Member lookup by email requested: email={}", email);

		MemberInfoResponse response = memberService.getMemberByEmail(email);
		return ResponseEntity.ok(response);
	}

	/**
	 * 회원 탈퇴(본인).
	 * <p>
	 * 인증된 회원 본인을 탈퇴 처리(soft delete)한다. 상태를 DELETED 로 전환하고
	 * 리프레시 토큰을 무효화한다.
	 * DELETE /api/members/me
	 */
	@DeleteMapping("/me")
	public ResponseEntity<Void> withdraw() {
		Long memberId = currentMemberId();
		log.info("Member withdrawal requested: memberId={}", memberId);

		memberService.withdraw(memberId);
		return ResponseEntity.noContent().build();
	}

	private Long currentMemberId() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		return Long.valueOf(authentication.getName());
	}
}
