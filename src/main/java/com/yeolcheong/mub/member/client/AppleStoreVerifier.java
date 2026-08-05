package com.yeolcheong.mub.member.client;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.Date;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yeolcheong.mub.member.config.AppleIapProperties;
import com.yeolcheong.mub.member.domain.MembershipPlatform;

import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Apple App Store Server API 기반 구매 검증기.
 * ES256 JWT(App Store Connect API 키)로 인증하여 거래 정보를 조회하고, 서명된 거래(JWS) payload 를 정규화한다.
 * 키/설정은 최초 검증 호출 시 지연 로딩하여, 자격증명이 없는 환경에서도 빈 생성은 실패하지 않게 한다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AppleStoreVerifier implements StorePurchaseVerifier {

	private static final String AUDIENCE = "appstoreconnect-v1";
	private static final long JWT_TTL_SECONDS = 1800;

	private final AppleIapProperties properties;
	private final WebClient.Builder webClientBuilder;
	private final ObjectMapper objectMapper = new ObjectMapper();

	private volatile PrivateKey privateKey;
	private volatile WebClient webClient;

	@Override
	public MembershipPlatform platform() {
		return MembershipPlatform.APPLE;
	}

	@Override
	public StoreVerificationResult verify(String productId, String purchaseToken) {
		try {
			String token = generateToken();
			JsonNode response = client().get()
				.uri("/inApps/v1/transactions/{id}", purchaseToken)
				.header("Authorization", "Bearer " + token)
				.retrieve()
				.bodyToMono(JsonNode.class)
				.block();

			if (response == null || !response.hasNonNull("signedTransactionInfo")) {
				log.warn("Apple verify — no signedTransactionInfo | transactionId={}", purchaseToken);
				return StoreVerificationResult.invalid();
			}

			JsonNode tx = decodeJwsPayload(response.get("signedTransactionInfo").asText());
			String verifiedProductId = tx.path("productId").asText(null);
			long expiresMs = tx.path("expiresDate").asLong(0);
			String originalTransactionId = tx.path("originalTransactionId").asText(purchaseToken);

			LocalDateTime expiresAt = expiresMs > 0
				? LocalDateTime.ofInstant(Instant.ofEpochMilli(expiresMs), ZoneId.systemDefault())
				: null;
			boolean valid = verifiedProductId != null && expiresAt != null && expiresAt.isAfter(LocalDateTime.now());

			log.info("Apple verify | productId={}, expiresAt={}, valid={}", verifiedProductId, expiresAt, valid);
			return StoreVerificationResult.builder()
				.valid(valid)
				.productId(verifiedProductId)
				.storeTransactionId(originalTransactionId)
				.expiresAt(expiresAt)
				.autoRenewing(valid)
				.build();

		} catch (Exception e) {
			log.error("Apple verify failed | transactionId={}", purchaseToken, e);
			return StoreVerificationResult.invalid();
		}
	}

	/** App Store Connect API 인증용 ES256 JWT 를 생성한다. */
	private String generateToken() {
		Instant now = Instant.now();
		return Jwts.builder()
			.header().keyId(properties.getKeyId()).type("JWT").and()
			.issuer(properties.getIssuerId())
			.issuedAt(Date.from(now))
			.expiration(Date.from(now.plusSeconds(JWT_TTL_SECONDS)))
			.audience().add(AUDIENCE).and()
			.claim("bid", properties.getBundleId())
			.signWith(loadPrivateKey(), Jwts.SIG.ES256)
			.compact();
	}

	/** JWS(헤더.payload.서명)의 payload 를 base64url 디코드하여 JSON 으로 반환한다. */
	private JsonNode decodeJwsPayload(String jws) throws Exception {
		String[] parts = jws.split("\\.");
		byte[] payload = Base64.getUrlDecoder().decode(parts[1]);
		return objectMapper.readTree(payload);
	}

	private PrivateKey loadPrivateKey() {
		if (privateKey == null) {
			synchronized (this) {
				if (privateKey == null) {
					privateKey = readEcPrivateKey(properties.getPrivateKeyPath());
				}
			}
		}
		return privateKey;
	}

	private PrivateKey readEcPrivateKey(String path) {
		try {
			String pem = Files.readString(Path.of(path))
				.replace("-----BEGIN PRIVATE KEY-----", "")
				.replace("-----END PRIVATE KEY-----", "")
				.replaceAll("\\s", "");
			byte[] der = Base64.getDecoder().decode(pem);
			return KeyFactory.getInstance("EC").generatePrivate(new PKCS8EncodedKeySpec(der));
		} catch (Exception e) {
			throw new IllegalStateException("Failed to load Apple .p8 private key", e);
		}
	}

	private WebClient client() {
		if (webClient == null) {
			synchronized (this) {
				if (webClient == null) {
					webClient = webClientBuilder.baseUrl(properties.getBaseUrl()).build();
				}
			}
		}
		return webClient;
	}
}
