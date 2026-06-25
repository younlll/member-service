package com.yeolcheong.mub.member.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

/**
 * group-service 연동 설정. 가입 축하 활동 이용권 발급을 group-service 에 위임할 때 사용한다.
 */
@Component
@ConfigurationProperties(prefix = "group-service")
@Getter
@Setter
public class GroupProperties {

	private String baseUrl;
}
