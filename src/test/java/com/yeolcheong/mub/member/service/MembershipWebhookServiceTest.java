package com.yeolcheong.mub.member.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.yeolcheong.mub.member.client.StorePurchaseVerifier;
import com.yeolcheong.mub.member.client.StoreVerificationResult;
import com.yeolcheong.mub.member.domain.Membership;
import com.yeolcheong.mub.member.domain.MembershipPlatform;
import com.yeolcheong.mub.member.domain.MembershipStatus;
import com.yeolcheong.mub.member.repository.MembershipRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("MembershipWebhookService")
class MembershipWebhookServiceTest {

	@Mock
	private StorePurchaseVerifier appleVerifier;
	@Mock
	private StorePurchaseVerifier googleVerifier;
	@Mock
	private MembershipRepository membershipRepository;

	private MembershipWebhookService service;

	@BeforeEach
	void setUp() {
		given(appleVerifier.platform()).willReturn(MembershipPlatform.APPLE);
		given(googleVerifier.platform()).willReturn(MembershipPlatform.GOOGLE);
		service = new MembershipWebhookService(List.of(appleVerifier, googleVerifier), membershipRepository);
	}

	@Test
	@DisplayName("handleGoogleNotification - renews membership to ACTIVE when the store confirms it is valid")
	void googleNotificationRenewsActive() {
		// given
		Membership membership = buildMembership("tok", MembershipStatus.ACTIVE, LocalDateTime.now().plusDays(1));
		given(membershipRepository.findByStoreTransactionId("tok")).willReturn(Optional.of(membership));
		LocalDateTime newExpiry = LocalDateTime.now().plusDays(30);
		given(googleVerifier.verify("pid", "tok")).willReturn(StoreVerificationResult.builder()
			.valid(true).productId("pid").storeTransactionId("tok").expiresAt(newExpiry).autoRenewing(true).build());

		// when
		service.handleGoogleNotification(googleData(2, "tok", "pid"));

		// then
		assertThat(membership.getStatus()).isEqualTo(MembershipStatus.ACTIVE);
		assertThat(membership.getExpiresAt()).isEqualTo(newExpiry);
	}

	@Test
	@DisplayName("handleGoogleNotification - marks membership EXPIRED when the store no longer considers it valid")
	void googleNotificationMarksExpired() {
		// given
		Membership membership = buildMembership("tok", MembershipStatus.ACTIVE, LocalDateTime.now().plusDays(1));
		given(membershipRepository.findByStoreTransactionId("tok")).willReturn(Optional.of(membership));
		given(googleVerifier.verify("pid", "tok")).willReturn(StoreVerificationResult.invalid());

		// when
		service.handleGoogleNotification(googleData(13, "tok", "pid"));

		// then
		assertThat(membership.getStatus()).isEqualTo(MembershipStatus.EXPIRED);
	}

	@Test
	@DisplayName("handleGoogleNotification - ignores notification for an unknown purchase token")
	void googleNotificationUnknownTokenIgnored() {
		given(membershipRepository.findByStoreTransactionId("nope")).willReturn(Optional.empty());

		service.handleGoogleNotification(googleData(2, "nope", "pid"));

		then(googleVerifier).should(never()).verify(any(), any());
	}

	@Test
	@DisplayName("handleAppleNotification - renews membership to ACTIVE from the signed transaction")
	void appleNotificationRenewsActive() {
		// given
		Membership membership = buildMembership("otx", MembershipStatus.ACTIVE, LocalDateTime.now().plusDays(1));
		given(membershipRepository.findByStoreTransactionId("otx")).willReturn(Optional.of(membership));
		LocalDateTime newExpiry = LocalDateTime.now().plusDays(30);
		given(appleVerifier.verify("com.mub.app.membership.monthly", "otx")).willReturn(StoreVerificationResult.builder()
			.valid(true).productId("com.mub.app.membership.monthly").storeTransactionId("otx")
			.expiresAt(newExpiry).autoRenewing(true).build());

		String txInfo = jws("{\"originalTransactionId\":\"otx\",\"productId\":\"com.mub.app.membership.monthly\"}");
		String signedPayload = jws("{\"notificationType\":\"DID_RENEW\",\"data\":{\"signedTransactionInfo\":\"" + txInfo + "\"}}");

		// when
		service.handleAppleNotification(signedPayload);

		// then
		assertThat(membership.getStatus()).isEqualTo(MembershipStatus.ACTIVE);
		assertThat(membership.getExpiresAt()).isEqualTo(newExpiry);
	}

	@Test
	@DisplayName("handleGoogleNotification - ignores null message data without failing")
	void googleNotificationNullDataIgnored() {
		service.handleGoogleNotification(null);

		then(membershipRepository).should(never()).findByStoreTransactionId(any());
	}

	private Membership buildMembership(String txId, MembershipStatus status, LocalDateTime expiresAt) {
		return Membership.builder()
			.id(1L).memberId(1L).platform(MembershipPlatform.GOOGLE)
			.storeTransactionId(txId).productId("pid").status(status)
			.startedAt(LocalDateTime.now().minusDays(1)).expiresAt(expiresAt).build();
	}

	/** 테스트용 JWS: 헤더/서명은 의미 없고 payload 만 base64url 로 담는다(서비스는 payload 만 디코드). */
	private String jws(String payloadJson) {
		String payload = Base64.getUrlEncoder().withoutPadding()
			.encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));
		return "e30." + payload + ".sig";
	}

	/** 테스트용 Google RTDN base64 data. */
	private String googleData(int notificationType, String purchaseToken, String subscriptionId) {
		String json = "{\"subscriptionNotification\":{\"notificationType\":" + notificationType
			+ ",\"purchaseToken\":\"" + purchaseToken + "\",\"subscriptionId\":\"" + subscriptionId + "\"}}";
		return Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
	}
}
