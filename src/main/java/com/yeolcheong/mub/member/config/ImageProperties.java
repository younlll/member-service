package com.yeolcheong.mub.member.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 이미지 저장/조회 설정.
 * <p>
 * 현재는 로컬 파일 시스템({@code uploadDir/profileDir})에 저장하고, 향후 서버 스토리지(S3 등)로
 * 전환하더라도 DB에는 스토리지 중립 상대 경로({@code filePath})만 남기고 {@code baseUrl}만 교체하면
 * 되도록 분리한다.
 *
 * @param uploadDir          <b>로컬 전용</b> 스토리지 루트 디렉터리 (예: images). 서버 스토리지 전환 시 미사용.
 * @param profileDir         프로필 이미지 하위 디렉터리 (예: profile) — 스토리지 중립 키의 접두사
 * @param baseUrl            공개 URL 베이스 (예: http://localhost:8083, 향후 CDN/스토리지 도메인)
 * @param urlPathPrefix      <b>로컬 전용</b> uploadDir 에 매핑되는 URL 접두사 (예: /images)
 * @param defaultProfilePath 기본 프로필 이미지의 상대 경로 (예: profile/default.png)
 */
@ConfigurationProperties(prefix = "app.image")
public record ImageProperties(
	String uploadDir,
	String profileDir,
	String baseUrl,
	String urlPathPrefix,
	String defaultProfilePath
) {

	/**
	 * 상대 경로(예: profile/uuid.png)를 공개 URL로 조합한다.
	 */
	public String toPublicUrl(String relativePath) {
		return baseUrl + urlPathPrefix + "/" + relativePath;
	}
}
