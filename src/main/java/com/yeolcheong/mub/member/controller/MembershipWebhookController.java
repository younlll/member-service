package com.yeolcheong.mub.member.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.yeolcheong.mub.member.dto.AppleNotificationRequest;
import com.yeolcheong.mub.member.dto.GoogleNotificationRequest;
import com.yeolcheong.mub.member.service.MembershipWebhookService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 멤버십 스토어 웹훅 수신(public). Apple/Google 서버가 구독 상태 변경(갱신·해지·만료)을 통지한다.
 * 처리 가능 여부와 무관하게 항상 200 으로 ack 하여 불필요한 재전송을 막는다.
 */
@RestController
@RequestMapping("/api/memberships/webhook")
@RequiredArgsConstructor
@Slf4j
public class MembershipWebhookController {

	private final MembershipWebhookService webhookService;

	/**
	 * Apple App Store Server Notifications V2 수신.
	 *
	 * @param request 서명 페이로드
	 * @return 200 OK
	 */
	@PostMapping("/apple")
	public ResponseEntity<Void> apple(@RequestBody AppleNotificationRequest request) {
		log.info("POST apple membership webhook");
		webhookService.handleAppleNotification(request.getSignedPayload());
		return ResponseEntity.ok().build();
	}

	/**
	 * Google Play RTDN(Pub/Sub push) 수신.
	 *
	 * @param request Pub/Sub 메시지(base64 data)
	 * @return 200 OK
	 */
	@PostMapping("/google")
	public ResponseEntity<Void> google(@RequestBody GoogleNotificationRequest request) {
		log.info("POST google membership webhook");
		String data = request.getMessage() != null ? request.getMessage().getData() : null;
		webhookService.handleGoogleNotification(data);
		return ResponseEntity.ok().build();
	}
}
