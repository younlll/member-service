package com.yeolcheong.mub.member.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.yeolcheong.mub.member.dto.AccountInfoResponse;
import com.yeolcheong.mub.member.dto.MemberInfoResponse;
import com.yeolcheong.mub.member.dto.NotificationSettingsResponse;
import com.yeolcheong.mub.member.dto.NotificationSettingsUpdateRequest;
import com.yeolcheong.mub.member.dto.ProfileResponse;
import com.yeolcheong.mub.member.dto.ProfileUpdateRequest;
import com.yeolcheong.mub.member.service.MemberService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
@Slf4j
public class MemberController {

	private final MemberService memberService;

	/**
	 * 내 프로필 조회(본인).
	 * GET /api/members/me
	 */
	@GetMapping("/me")
	public ResponseEntity<ProfileResponse> getMyProfile() {
		Long memberId = currentMemberId();
		log.info("My profile lookup requested: memberId={}", memberId);

		return ResponseEntity.ok(memberService.getMyProfile(memberId));
	}

	/**
	 * 내 프로필 수정(본인) — 닉네임, 한줄소개, 활동 지역, 관심사.
	 * PUT /api/members/me/profile
	 */
	@PutMapping("/me/profile")
	public ResponseEntity<ProfileResponse> updateProfile(@Valid @RequestBody ProfileUpdateRequest request) {
		Long memberId = currentMemberId();
		log.info("Profile update requested: memberId={}", memberId);

		return ResponseEntity.ok(memberService.updateProfile(memberId, request));
	}

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
	 * 내 계정 정보 조회(본인) — 연결된 카카오 계정(email·snsProvider) 등.
	 * GET /api/members/me/account
	 */
	@GetMapping("/me/account")
	public ResponseEntity<AccountInfoResponse> getMyAccount() {
		Long memberId = currentMemberId();
		log.info("Account info lookup requested: memberId={}", memberId);

		return ResponseEntity.ok(memberService.getMyAccount(memberId));
	}

	/**
	 * 알림 설정 조회(본인) — 광고성(마케팅) 수신 동의 여부.
	 * GET /api/members/me/notification-settings
	 */
	@GetMapping("/me/notification-settings")
	public ResponseEntity<NotificationSettingsResponse> getNotificationSettings() {
		Long memberId = currentMemberId();
		log.info("Notification settings lookup requested: memberId={}", memberId);

		return ResponseEntity.ok(memberService.getNotificationSettings(memberId));
	}

	/**
	 * 알림 설정 수정(본인) — 광고성(마케팅) 수신 동의 토글.
	 * PUT /api/members/me/notification-settings
	 */
	@PutMapping("/me/notification-settings")
	public ResponseEntity<NotificationSettingsResponse> updateNotificationSettings(
		@Valid @RequestBody NotificationSettingsUpdateRequest request) {
		Long memberId = currentMemberId();
		log.info("Notification settings update requested: memberId={}", memberId);

		return ResponseEntity.ok(
			memberService.updateNotificationSettings(memberId, request.getMarketingAgreed()));
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
