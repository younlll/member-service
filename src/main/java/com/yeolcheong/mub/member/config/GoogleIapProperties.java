package com.yeolcheong.mub.member.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

/**
 * Google Play Developer API 연동 설정. 실제 값은 gitignore 되는 properties 로만 관리한다.
 * {@code serviceAccountKeyPath} 가 가리키는 서비스계정 JSON 은 커밋/이미지에 포함하지 않는다.
 */
@Component
@ConfigurationProperties(prefix = "iap.google")
@Getter
@Setter
public class GoogleIapProperties {

	/** Android 앱 패키지명(예: com.aeon.mub). */
	private String packageName;

	/** 서비스계정 키 JSON 파일 경로. */
	private String serviceAccountKeyPath;

	/** Google Play Developer API base URL. */
	private String baseUrl;
}
