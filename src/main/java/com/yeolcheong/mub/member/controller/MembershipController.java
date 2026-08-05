package com.yeolcheong.mub.member.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.yeolcheong.mub.member.dto.MembershipProductResponse;
import com.yeolcheong.mub.member.dto.MembershipPurchaseRequest;
import com.yeolcheong.mub.member.dto.MembershipResponse;
import com.yeolcheong.mub.member.service.MembershipPurchaseService;
import com.yeolcheong.mub.member.service.MembershipService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 멤버십(구독) API. 인증된 회원이 멤버십 상품을 조회하고, 스토어 구매를 검증해 멤버십을 활성화한다.
 */
@RestController
@RequestMapping("/api/memberships")
@RequiredArgsConstructor
@Slf4j
public class MembershipController {

	private final MembershipService membershipService;
	private final MembershipPurchaseService membershipPurchaseService;

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

	/**
	 * 내 멤버십 조회('내 멤버십' 화면). 현재 이용 중인 멤버십을 반환한다.
	 *
	 * @return 내 멤버십 정보
	 */
	@GetMapping("/me")
	public ResponseEntity<MembershipResponse> getMyMembership() {
		Long memberId = currentMemberId();
		log.info("GET my membership | memberId={}", memberId);
		return ResponseEntity.ok(membershipService.getMyMembership(memberId));
	}

	/**
	 * 멤버십 구매 검증. 앱이 스토어 결제 완료 후 전달한 정보를 서버가 검증하고 멤버십을 활성화한다.
	 *
	 * @param request 플랫폼·상품ID·구매토큰
	 * @return 활성화된 멤버십
	 */
	@PostMapping("/purchase")
	public ResponseEntity<MembershipResponse> purchase(@RequestBody @Valid MembershipPurchaseRequest request) {
		Long memberId = currentMemberId();
		log.info("POST membership purchase | memberId={}, platform={}", memberId, request.getPlatform());
		return ResponseEntity.ok(membershipPurchaseService.verifyAndActivate(memberId, request));
	}

	/** 인증 컨텍스트에서 현재 회원 ID(JWT sub)를 얻는다. */
	private Long currentMemberId() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		return Long.valueOf(authentication.getName());
	}
}
