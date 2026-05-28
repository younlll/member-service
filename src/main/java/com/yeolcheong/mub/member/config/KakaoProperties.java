package com.yeolcheong.mub.member.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Component
@ConfigurationProperties(prefix = "kakao")
@Getter
@Setter
public class KakaoProperties {

	private String authUrl;
	private String clientId;
	private String redirectUri;
	private String tokenUrl;
	private String userInfoUrl;
}
