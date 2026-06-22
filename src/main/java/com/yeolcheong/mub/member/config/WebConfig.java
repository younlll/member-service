package com.yeolcheong.mub.member.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import lombok.RequiredArgsConstructor;

/**
 * 정적 리소스(프로필 이미지) 서빙 설정.
 * <p>
 * {@code urlPathPrefix}(예: /images) 요청을 로컬 스토리지 디렉터리({@code uploadDir})로 매핑한다.
 * 향후 오브젝트 스토리지로 전환하면 본 핸들러 대신 CDN/스토리지 URL을 사용하면 된다.
 */
@Configuration
@EnableConfigurationProperties(ImageProperties.class)
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

	private final ImageProperties imageProperties;

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		registry.addResourceHandler(imageProperties.urlPathPrefix() + "/**")
			.addResourceLocations("file:" + imageProperties.uploadDir() + "/");
	}
}
