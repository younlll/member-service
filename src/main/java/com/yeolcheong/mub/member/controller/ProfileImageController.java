package com.yeolcheong.mub.member.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.yeolcheong.mub.member.dto.ProfileImageResponse;
import com.yeolcheong.mub.member.service.ProfileImageService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 인증된 회원 본인의 프로필 이미지 관리 API.
 * <p>
 * 등록(POST) / 수정(PUT) / 삭제(DELETE, 기본 이미지로 복귀) / 조회(GET) 를 제공한다.
 * 대상 회원은 인증 컨텍스트(JWT)에서 식별한다.
 */
@RestController
@RequestMapping("/api/members/me/profile-image")
@RequiredArgsConstructor
@Slf4j
public class ProfileImageController {

	private final ProfileImageService profileImageService;

	/**
	 * 프로필 이미지 등록(또는 교체).
	 * POST /api/members/me/profile-image
	 */
	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<ProfileImageResponse> register(@RequestParam("file") MultipartFile file) {
		Long memberId = currentMemberId();
		log.info("Profile image register requested: memberId={}", memberId);

		ProfileImageResponse response = profileImageService.register(memberId, file);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	/**
	 * 프로필 이미지 수정.
	 * PUT /api/members/me/profile-image
	 */
	@PutMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<ProfileImageResponse> update(@RequestParam("file") MultipartFile file) {
		Long memberId = currentMemberId();
		log.info("Profile image update requested: memberId={}", memberId);

		ProfileImageResponse response = profileImageService.update(memberId, file);
		return ResponseEntity.ok(response);
	}

	/**
	 * 프로필 이미지 삭제(기본 이미지로 복귀).
	 * DELETE /api/members/me/profile-image
	 */
	@DeleteMapping
	public ResponseEntity<ProfileImageResponse> delete() {
		Long memberId = currentMemberId();
		log.info("Profile image delete requested: memberId={}", memberId);

		ProfileImageResponse response = profileImageService.delete(memberId);
		return ResponseEntity.ok(response);
	}

	/**
	 * 프로필 이미지 조회.
	 * GET /api/members/me/profile-image
	 */
	@GetMapping
	public ResponseEntity<ProfileImageResponse> get() {
		Long memberId = currentMemberId();
		log.info("Profile image lookup requested: memberId={}", memberId);

		ProfileImageResponse response = profileImageService.get(memberId);
		return ResponseEntity.ok(response);
	}

	private Long currentMemberId() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		return Long.valueOf(authentication.getName());
	}
}
