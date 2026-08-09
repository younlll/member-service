package com.yeolcheong.mub.member.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * CORS 허용 출처 설정.
 * <p>
 * 배포 환경마다 프론트 도메인이 달라지므로 코드에 고정하지 않고 설정값으로 주입받는다.
 * 값은 콤마로 구분된 목록이며, 와일드카드 패턴(예: {@code http://localhost:*})도 사용할 수 있도록
 * {@code allowedOriginPatterns} 로 등록한다.
 *
 * @param allowedOrigins 허용할 출처 목록. 자격증명(쿠키·Authorization)을 함께 보내므로
 *                       {@code *} 단독 값은 브라우저가 거부한다 — 실제 도메인을 나열해야 한다.
 */
@ConfigurationProperties(prefix = "app.cors")
public record CorsProperties(
	List<String> allowedOrigins
) {

	/**
	 * 미설정 시 로컬 개발 기본값으로 대체한다(운영에서는 항상 명시적으로 주입된다).
	 */
	public List<String> resolvedOrigins() {
		if (allowedOrigins == null || allowedOrigins.isEmpty()) {
			return List.of("http://localhost:3000", "capacitor://localhost", "ionic://localhost");
		}
		return allowedOrigins;
	}
}
