package com.yeolcheong.mub.member.client;

import static org.assertj.core.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yeolcheong.mub.member.config.GoogleIapProperties;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

/**
 * GoogleStoreVerifier 통합 테스트(MockWebServer). 실제 RS256 JWT → OAuth2 토큰 교환 → subscriptionsv2 조회 →
 * 응답 파싱을 스텁 Google 응답으로 검증한다(라이브 스토어 불필요). 토큰 URL 은 설정으로 mock 을 가리킨다.
 */
@DisplayName("GoogleStoreVerifier integration (MockWebServer)")
class GoogleStoreVerifierTest {

	private final ObjectMapper objectMapper = new ObjectMapper();
	private MockWebServer server;
	private GoogleStoreVerifier verifier;

	@BeforeEach
	void setUp() throws Exception {
		server = new MockWebServer();
		server.start();

		GoogleIapProperties properties = new GoogleIapProperties();
		properties.setPackageName("com.aeon.mub");
		properties.setBaseUrl(server.url("/").toString());
		properties.setTokenUrl(server.url("/token").toString());
		properties.setServiceAccountKeyPath(writeTempServiceAccount());

		verifier = new GoogleStoreVerifier(properties, WebClient.builder());
	}

	@AfterEach
	void tearDown() throws Exception {
		server.shutdown();
	}

	@Test
	@DisplayName("verify - returns a valid result when the subscription state is ACTIVE")
	void verifyValid() throws Exception {
		server.enqueue(new MockResponse()
			.addHeader("Content-Type", "application/json")
			.setBody(objectMapper.writeValueAsString(Map.of("access_token", "at-123", "expires_in", 3600))));
		String expiry = OffsetDateTime.now().plusDays(30).toString();
		String subscriptionBody = "{\"subscriptionState\":\"SUBSCRIPTION_STATE_ACTIVE\",\"lineItems\":[{"
			+ "\"productId\":\"mub_membership_monthly\",\"expiryTime\":\"" + expiry + "\","
			+ "\"autoRenewingPlan\":{\"autoRenewEnabled\":true}}]}";
		server.enqueue(new MockResponse()
			.addHeader("Content-Type", "application/json")
			.setBody(subscriptionBody));

		StoreVerificationResult result = verifier.verify("mub_membership_monthly", "purchase-token-1");

		assertThat(result.isValid()).isTrue();
		assertThat(result.getProductId()).isEqualTo("mub_membership_monthly");
		assertThat(result.getStoreTransactionId()).isEqualTo("purchase-token-1");
		assertThat(result.getExpiresAt()).isAfter(LocalDateTime.now());
		assertThat(result.isAutoRenewing()).isTrue();
	}

	@Test
	@DisplayName("verify - returns invalid when the subscription is not active")
	void verifyNotActive() throws Exception {
		server.enqueue(new MockResponse()
			.addHeader("Content-Type", "application/json")
			.setBody(objectMapper.writeValueAsString(Map.of("access_token", "at-123", "expires_in", 3600))));
		String expiry = OffsetDateTime.now().minusDays(1).toString();
		String subscriptionBody = "{\"subscriptionState\":\"SUBSCRIPTION_STATE_EXPIRED\",\"lineItems\":[{"
			+ "\"productId\":\"mub_membership_monthly\",\"expiryTime\":\"" + expiry + "\","
			+ "\"autoRenewingPlan\":{\"autoRenewEnabled\":false}}]}";
		server.enqueue(new MockResponse()
			.addHeader("Content-Type", "application/json")
			.setBody(subscriptionBody));

		StoreVerificationResult result = verifier.verify("mub_membership_monthly", "purchase-token-1");

		assertThat(result.isValid()).isFalse();
	}

	private String writeTempServiceAccount() throws Exception {
		KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
		generator.initialize(2048);
		PrivateKey privateKey = generator.generateKeyPair().getPrivate();
		String keyPem = "-----BEGIN PRIVATE KEY-----\n"
			+ Base64.getMimeEncoder().encodeToString(privateKey.getEncoded())
			+ "\n-----END PRIVATE KEY-----\n";
		String json = objectMapper.writeValueAsString(Map.of(
			"type", "service_account",
			"client_email", "test@mub.iam.gserviceaccount.com",
			"private_key", keyPem));
		Path file = Files.createTempFile("google-sa", ".json");
		Files.writeString(file, json);
		file.toFile().deleteOnExit();
		return file.toString();
	}
}
