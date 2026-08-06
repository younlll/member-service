package com.yeolcheong.mub.member.client;

import static org.assertj.core.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.spec.ECGenParameterSpec;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yeolcheong.mub.member.config.AppleIapProperties;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

/**
 * AppleStoreVerifier 통합 테스트(MockWebServer). 실제 ES256 JWT 서명 → HTTP 호출 → 서명 거래(JWS) 파싱을
 * 스텁 App Store 응답으로 검증한다(라이브 스토어 불필요).
 */
@DisplayName("AppleStoreVerifier integration (MockWebServer)")
class AppleStoreVerifierTest {

	private final ObjectMapper objectMapper = new ObjectMapper();
	private MockWebServer server;
	private AppleStoreVerifier verifier;

	@BeforeEach
	void setUp() throws Exception {
		server = new MockWebServer();
		server.start();

		AppleIapProperties properties = new AppleIapProperties();
		properties.setKeyId("KID12345");
		properties.setIssuerId("issuer-uuid");
		properties.setBundleId("com.mub.app");
		properties.setBaseUrl(server.url("/").toString());
		properties.setPrivateKeyPath(writeTempEcKey());

		verifier = new AppleStoreVerifier(properties, WebClient.builder());
	}

	@AfterEach
	void tearDown() throws Exception {
		server.shutdown();
	}

	@Test
	@DisplayName("verify - returns a valid result and signs the request with an ES256 JWT (kid header)")
	void verifyValid() throws Exception {
		long expiresMs = System.currentTimeMillis() + 30L * 86_400 * 1000;
		String txJws = jws(objectMapper.writeValueAsString(Map.of(
			"productId", "com.mub.app.membership.monthly",
			"expiresDate", expiresMs,
			"originalTransactionId", "otx-123")));
		server.enqueue(new MockResponse()
			.addHeader("Content-Type", "application/json")
			.setBody(objectMapper.writeValueAsString(Map.of("signedTransactionInfo", txJws))));

		StoreVerificationResult result = verifier.verify("com.mub.app.membership.monthly", "otx-123");

		assertThat(result.isValid()).isTrue();
		assertThat(result.getProductId()).isEqualTo("com.mub.app.membership.monthly");
		assertThat(result.getStoreTransactionId()).isEqualTo("otx-123");
		assertThat(result.getExpiresAt()).isAfter(LocalDateTime.now());

		// 요청이 ES256 JWT(Bearer) 로 서명되었고 헤더 kid 가 설정되었는지 확인
		RecordedRequest request = server.takeRequest();
		String authorization = request.getHeader("Authorization");
		assertThat(authorization).startsWith("Bearer ");
		JsonNode header = objectMapper.readTree(
			Base64.getUrlDecoder().decode(authorization.substring(7).split("\\.")[0]));
		assertThat(header.get("kid").asText()).isEqualTo("KID12345");
	}

	@Test
	@DisplayName("verify - returns invalid when the transaction is already expired")
	void verifyExpired() throws Exception {
		long expiresMs = System.currentTimeMillis() - 1000;
		String txJws = jws(objectMapper.writeValueAsString(Map.of(
			"productId", "com.mub.app.membership.monthly",
			"expiresDate", expiresMs,
			"originalTransactionId", "otx-123")));
		server.enqueue(new MockResponse()
			.addHeader("Content-Type", "application/json")
			.setBody(objectMapper.writeValueAsString(Map.of("signedTransactionInfo", txJws))));

		StoreVerificationResult result = verifier.verify("com.mub.app.membership.monthly", "otx-123");

		assertThat(result.isValid()).isFalse();
	}

	private String writeTempEcKey() throws Exception {
		KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
		generator.initialize(new ECGenParameterSpec("secp256r1"));
		PrivateKey privateKey = generator.generateKeyPair().getPrivate();
		String pem = "-----BEGIN PRIVATE KEY-----\n"
			+ Base64.getMimeEncoder().encodeToString(privateKey.getEncoded())
			+ "\n-----END PRIVATE KEY-----\n";
		Path file = Files.createTempFile("apple-key", ".p8");
		Files.writeString(file, pem);
		file.toFile().deleteOnExit();
		return file.toString();
	}

	/** 테스트용 JWS: 헤더/서명은 임의, payload 만 base64url 로 담는다. */
	private String jws(String payloadJson) {
		String header = base64Url("{\"alg\":\"ES256\"}");
		String payload = base64Url(payloadJson);
		return header + "." + payload + ".signature";
	}

	private String base64Url(String value) {
		return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
	}
}
