package com.yeolcheong.mub.member.client;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.Date;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yeolcheong.mub.member.config.GoogleIapProperties;
import com.yeolcheong.mub.member.domain.MembershipPlatform;

import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Google Play Developer API 기반 구매 검증기.
 * 서비스계정 JSON 으로 RS256 JWT 를 만들어 OAuth2 액세스 토큰을 발급받고, subscriptionsv2 로 구독을 조회한다.
 * 자격증명은 최초 검증 호출 시 지연 로딩한다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GoogleStoreVerifier implements StorePurchaseVerifier {

	private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
	private static final String SCOPE = "https://www.googleapis.com/auth/androidpublisher";
	private static final String ACTIVE_STATE = "SUBSCRIPTION_STATE_ACTIVE";
	private static final long JWT_TTL_SECONDS = 3600;

	private final GoogleIapProperties properties;
	private final WebClient.Builder webClientBuilder;
	private final ObjectMapper objectMapper = new ObjectMapper();

	private volatile String clientEmail;
	private volatile PrivateKey privateKey;
	private volatile WebClient apiClient;
	private volatile WebClient tokenClient;

	@Override
	public MembershipPlatform platform() {
		return MembershipPlatform.GOOGLE;
	}

	@Override
	public StoreVerificationResult verify(String productId, String purchaseToken) {
		try {
			String accessToken = fetchAccessToken();
			JsonNode response = apiClient().get()
				.uri("/androidpublisher/v3/applications/{pkg}/purchases/subscriptionsv2/tokens/{token}",
					properties.getPackageName(), purchaseToken)
				.header("Authorization", "Bearer " + accessToken)
				.retrieve()
				.bodyToMono(JsonNode.class)
				.block();

			if (response == null || !response.has("lineItems")) {
				log.warn("Google verify — no lineItems | token={}", purchaseToken);
				return StoreVerificationResult.invalid();
			}

			JsonNode lineItem = response.path("lineItems").path(0);
			String verifiedProductId = lineItem.path("productId").asText(null);
			String expiryTime = lineItem.path("expiryTime").asText(null);
			String state = response.path("subscriptionState").asText(null);

			LocalDateTime expiresAt = expiryTime != null
				? OffsetDateTime.parse(expiryTime).atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime()
				: null;
			boolean valid = ACTIVE_STATE.equals(state) && verifiedProductId != null && expiresAt != null;
			boolean autoRenewing = lineItem.path("autoRenewingPlan").path("autoRenewEnabled").asBoolean(false);

			log.info("Google verify | productId={}, state={}, expiresAt={}, valid={}",
				verifiedProductId, state, expiresAt, valid);
			return StoreVerificationResult.builder()
				.valid(valid)
				.productId(verifiedProductId)
				.storeTransactionId(purchaseToken)
				.expiresAt(expiresAt)
				.autoRenewing(autoRenewing)
				.build();

		} catch (Exception e) {
			log.error("Google verify failed | token={}", purchaseToken, e);
			return StoreVerificationResult.invalid();
		}
	}

	/** 서비스계정 RS256 JWT 로 OAuth2 액세스 토큰을 교환한다. */
	private String fetchAccessToken() {
		loadServiceAccount();
		Instant now = Instant.now();
		String assertion = Jwts.builder()
			.issuer(clientEmail)
			.audience().add(TOKEN_URL).and()
			.claim("scope", SCOPE)
			.issuedAt(Date.from(now))
			.expiration(Date.from(now.plusSeconds(JWT_TTL_SECONDS)))
			.signWith(privateKey, Jwts.SIG.RS256)
			.compact();

		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("grant_type", "urn:ietf:params:oauth:grant-type:jwt-bearer");
		form.add("assertion", assertion);

		JsonNode token = tokenClient().post()
			.contentType(MediaType.APPLICATION_FORM_URLENCODED)
			.body(BodyInserters.fromFormData(form))
			.retrieve()
			.bodyToMono(JsonNode.class)
			.block();

		if (token == null || !token.hasNonNull("access_token")) {
			throw new IllegalStateException("Failed to obtain Google access token");
		}
		return token.get("access_token").asText();
	}

	private void loadServiceAccount() {
		if (privateKey == null) {
			synchronized (this) {
				if (privateKey == null) {
					readServiceAccount(properties.getServiceAccountKeyPath());
				}
			}
		}
	}

	private void readServiceAccount(String path) {
		try {
			JsonNode json = objectMapper.readTree(Files.readString(Path.of(path)));
			clientEmail = json.get("client_email").asText();
			String pem = json.get("private_key").asText()
				.replace("-----BEGIN PRIVATE KEY-----", "")
				.replace("-----END PRIVATE KEY-----", "")
				.replaceAll("\\s", "");
			byte[] der = Base64.getDecoder().decode(pem);
			privateKey = KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(der));
		} catch (Exception e) {
			throw new IllegalStateException("Failed to load Google service account key", e);
		}
	}

	private WebClient apiClient() {
		if (apiClient == null) {
			synchronized (this) {
				if (apiClient == null) {
					apiClient = webClientBuilder.baseUrl(properties.getBaseUrl()).build();
				}
			}
		}
		return apiClient;
	}

	private WebClient tokenClient() {
		if (tokenClient == null) {
			synchronized (this) {
				if (tokenClient == null) {
					tokenClient = webClientBuilder.baseUrl(TOKEN_URL).build();
				}
			}
		}
		return tokenClient;
	}
}
