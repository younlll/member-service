package com.yeolcheong.mub.member.service;

import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yeolcheong.mub.member.client.StorePurchaseVerifier;
import com.yeolcheong.mub.member.client.StoreVerificationResult;
import com.yeolcheong.mub.member.domain.Membership;
import com.yeolcheong.mub.member.domain.MembershipPlatform;
import com.yeolcheong.mub.member.domain.MembershipStatus;
import com.yeolcheong.mub.member.repository.MembershipRepository;

import lombok.extern.slf4j.Slf4j;

/**
 * 멤버십 스토어 웹훅 처리(Apple ASSN V2 / Google RTDN).
 * 웹훅 페이로드는 신뢰하지 않고 <b>거래 식별자만 추출</b>한 뒤, 스토어에 재조회(B2 검증기)하여 상태를 확정한다.
 * 갱신/해지/만료를 멤버십에 멱등하게 반영한다.
 */
@Service
@Transactional
@Slf4j
public class MembershipWebhookService {

	private final Map<MembershipPlatform, StorePurchaseVerifier> verifiers;
	private final MembershipRepository membershipRepository;
	private final ObjectMapper objectMapper = new ObjectMapper();

	public MembershipWebhookService(
		List<StorePurchaseVerifier> verifierList, MembershipRepository membershipRepository) {
		this.verifiers = verifierList.stream()
			.collect(Collectors.toMap(StorePurchaseVerifier::platform, Function.identity()));
		this.membershipRepository = membershipRepository;
	}

	/**
	 * Apple ASSN V2 알림 처리. 서명 페이로드에서 originalTransactionId·productId 를 추출해 동기화한다.
	 *
	 * @param signedPayload Apple 서명 페이로드(JWS)
	 */
	public void handleAppleNotification(String signedPayload) {
		try {
			JsonNode payload = decodeJwsPayload(signedPayload);
			String notificationType = payload.path("notificationType").asText(null);
			JsonNode transaction = decodeJwsPayload(payload.path("data").path("signedTransactionInfo").asText());
			String originalTransactionId = transaction.path("originalTransactionId").asText(null);
			String productId = transaction.path("productId").asText(null);

			log.info("Apple notification | type={}, txId={}", notificationType, originalTransactionId);
			syncMembership(MembershipPlatform.APPLE, originalTransactionId, productId);
		} catch (Exception e) {
			log.error("Apple notification handling failed", e);
		}
	}

	/**
	 * Google RTDN 알림 처리. base64 data 에서 purchaseToken·subscriptionId 를 추출해 동기화한다.
	 *
	 * @param messageData base64 인코딩된 알림 JSON
	 */
	public void handleGoogleNotification(String messageData) {
		try {
			if (messageData == null) {
				log.warn("Google notification ignored — no message data");
				return;
			}
			JsonNode data = objectMapper.readTree(Base64.getDecoder().decode(messageData));
			JsonNode subscription = data.path("subscriptionNotification");
			String purchaseToken = subscription.path("purchaseToken").asText(null);
			String subscriptionId = subscription.path("subscriptionId").asText(null);
			int notificationType = subscription.path("notificationType").asInt(0);

			log.info("Google notification | type={}, token={}", notificationType, purchaseToken);
			syncMembership(MembershipPlatform.GOOGLE, purchaseToken, subscriptionId);
		} catch (Exception e) {
			log.error("Google notification handling failed", e);
		}
	}

	/**
	 * 거래 식별자로 멤버십을 찾아 스토어 재조회 결과로 상태를 확정한다.
	 * 유효하면 ACTIVE(만료일 갱신), 무효(만료·해지·환불)면 EXPIRED 로 전환한다.
	 */
	private void syncMembership(MembershipPlatform platform, String storeTransactionId, String productId) {
		if (storeTransactionId == null) {
			log.warn("Webhook ignored — no store transaction id | platform={}", platform);
			return;
		}
		Membership membership = membershipRepository.findByStoreTransactionId(storeTransactionId).orElse(null);
		if (membership == null) {
			log.warn("Webhook ignored — unknown membership | txId={}", storeTransactionId);
			return;
		}
		StorePurchaseVerifier verifier = verifiers.get(platform);
		if (verifier == null) {
			log.warn("Webhook ignored — no verifier | platform={}", platform);
			return;
		}

		StoreVerificationResult result = verifier.verify(productId, storeTransactionId);
		if (result.isValid()) {
			membership.renew(MembershipStatus.ACTIVE, result.getExpiresAt());
			log.info("Membership synced ACTIVE | txId={}, expiresAt={}", storeTransactionId, result.getExpiresAt());
		} else {
			membership.renew(MembershipStatus.EXPIRED, membership.getExpiresAt());
			log.info("Membership synced EXPIRED | txId={}", storeTransactionId);
		}
	}

	/** JWS(헤더.payload.서명)의 payload 를 base64url 디코드하여 JSON 으로 반환한다. */
	private JsonNode decodeJwsPayload(String jws) throws Exception {
		String[] parts = jws.split("\\.");
		return objectMapper.readTree(Base64.getUrlDecoder().decode(parts[1]));
	}
}
