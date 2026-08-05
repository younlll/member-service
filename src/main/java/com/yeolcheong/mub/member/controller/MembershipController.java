package com.yeolcheong.mub.member.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.yeolcheong.mub.member.dto.MembershipProductResponse;
import com.yeolcheong.mub.member.service.MembershipService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 멤버십(구독) API. 인증된 회원이 멤버십 결제 소개 화면에서 상품 정보를 조회한다.
 */
@RestController
@RequestMapping("/api/memberships")
@RequiredArgsConstructor
@Slf4j
public class MembershipController {

	private final MembershipService membershipService;

	/**
	 * 현재 판매 중인 멤버십 상품 조회(기수·모집기간·월 금액·혜택·현재 가입자 수·스토어 상품 ID).
	 *
	 * @return 멤버십 상품 정보
	 */
	@GetMapping("/product")
	public ResponseEntity<MembershipProductResponse> getMembershipProduct() {
		log.info("GET membership product");
		return ResponseEntity.ok(membershipService.getActiveMembershipProduct());
	}
}
