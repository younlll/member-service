package com.yeolcheong.mub.member.controller;

import org.springframework.http.ResponseEntity;
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
	 * Retrieve member information by email.
	 *
	 * @param email the email address of the member to look up
	 * @return a ResponseEntity containing the MemberInfoResponse for the specified email
	 */
	@GetMapping
	public ResponseEntity<MemberInfoResponse> getMemberByEmail(
		@RequestParam String email) {
		log.info("이메일로 회원 조회 요청: email={}", email);

		MemberInfoResponse response = memberService.getMemberByEmail(email);
		return ResponseEntity.ok(response);
	}
}
