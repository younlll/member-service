package com.yeolcheong.mub.member.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

/**
 * Apple App Store Server API 연동 설정. 실제 값은 gitignore 되는 properties 로만 관리한다.
 * {@code privateKeyPath} 가 가리키는 .p8 파일은 커밋/이미지에 포함하지 않는다.
 */
@Component
@ConfigurationProperties(prefix = "iap.apple")
@Getter
@Setter
public class AppleIapProperties {

	/** App Store Connect API 키 ID(.p8 파일명 접미사). */
	private String keyId;

	/** App Store Connect Issuer ID. */
	private String issuerId;

	/** 앱 Bundle ID(예: com.mub.app). */
	private String bundleId;

	/** .p8 개인키(PKCS#8, EC) 파일 경로. */
	private String privateKeyPath;

	/** App Store Server API base URL(운영/샌드박스). */
	private String baseUrl;
}
