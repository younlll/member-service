package com.yeolcheong.mub.member.client;

import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.yeolcheong.mub.member.config.GroupProperties;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * group-service 호출 클라이언트.
 * 활동 이용권은 group-service 가 소유하므로, 가입 축하 이용권 발급을 group-service 에 위임한다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GroupClient {

	private final GroupProperties groupProperties;
	private final WebClient webClient;

	/**
	 * 가입 축하 활동 이용권 발급 요청(group-service 내부 API).
	 * <p>
	 * 호출 실패는 호출자(온보딩)가 격리 처리하므로 예외를 그대로 전파한다.
	 *
	 * @param memberId 발급 대상 회원 ID
	 */
	public void issueWelcomeVoucher(Long memberId) {
		webClient.post()
			.uri(groupProperties.getBaseUrl() + "/api/internal/vouchers/welcome")
			.contentType(MediaType.APPLICATION_JSON)
			.bodyValue(Map.of("memberId", memberId))
			.retrieve()
			.toBodilessEntity()
			.block();

		log.info("Welcome voucher issuance delegated to group-service: memberId={}", memberId);
	}
}
