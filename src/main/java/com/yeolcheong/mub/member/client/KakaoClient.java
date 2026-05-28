package com.yeolcheong.mub.member.client;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.yeolcheong.mub.member.config.KakaoProperties;
import com.yeolcheong.mub.member.dto.LoginTokenResponse;
import com.yeolcheong.mub.member.dto.SnsUserInfoResponse;
import com.yeolcheong.mub.member.exception.ErrorCode;
import com.yeolcheong.mub.member.exception.MemberServiceApiException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class KakaoClient {

	private final KakaoProperties kakaoProperties;
	private final WebClient webClient;

	public String buildAuthorizationUrl() {
		return kakaoProperties.getAuthUrl()
			+ "?client_id=" + kakaoProperties.getClientId()
			+ "&redirect_uri=" + kakaoProperties.getRedirectUri()
			+ "&response_type=code";
	}

	public LoginTokenResponse fetchAccessToken(String code) {
		log.info("카카오 Access Token 요청");

		MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
		params.add("grant_type", "authorization_code");
		params.add("client_id", kakaoProperties.getClientId());
		params.add("redirect_uri", kakaoProperties.getRedirectUri());
		params.add("code", code);

		try {
			LoginTokenResponse response = webClient.post()
				.uri(kakaoProperties.getTokenUrl())
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.bodyValue(params)
				.retrieve()
				.bodyToMono(LoginTokenResponse.class)
				.block();

			if (response == null) {
				log.error("카카오 토큰 응답이 null입니다");
				throw new MemberServiceApiException("토큰 응답이 없습니다.", ErrorCode.EXTERNAL_API_ERROR);
			}

			log.info("카카오 Access Token 발급 성공");
			return response;
		} catch (WebClientResponseException e) {
			log.error("Access Token 발급 실패: status={}", e.getStatusCode());
			throw mapTokenException(e);
		} catch (MemberServiceApiException e) {
			throw e;
		} catch (Exception e) {
			log.error("카카오 토큰 발급 중 예외 발생", e);
			throw new MemberServiceApiException("카카오 API 호출 중 오류가 발생했습니다", ErrorCode.EXTERNAL_API_ERROR);
		}
	}

	public SnsUserInfoResponse fetchUserInfo(String accessToken) {
		log.info("카카오 사용자 정보 조회 시작");

		try {
			SnsUserInfoResponse response = webClient.get()
				.uri(kakaoProperties.getUserInfoUrl())
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
				.retrieve()
				.bodyToMono(SnsUserInfoResponse.class)
				.block();

			if (response == null) {
				log.error("카카오 사용자 정보 응답이 null입니다");
				throw new MemberServiceApiException("카카오 사용자 정보 응답이 없습니다.", ErrorCode.EXTERNAL_API_ERROR);
			}

			log.info("카카오 사용자 정보 조회 성공: kakaoId={}", response.getId());
			return response;
		} catch (WebClientResponseException e) {
			log.error("카카오 사용자 정보 조회 실패: status={}", e.getStatusCode());
			if (e.getStatusCode().value() == 401) {
				throw new MemberServiceApiException("유효하지 않은 카카오 토큰입니다", ErrorCode.INVALID_TOKEN);
			}
			throw new MemberServiceApiException("카카오 사용자 정보 조회에 실패했습니다", ErrorCode.EXTERNAL_API_ERROR);
		} catch (MemberServiceApiException e) {
			throw e;
		} catch (Exception e) {
			log.error("카카오 사용자 정보 조회 중 예외 발생", e);
			throw new MemberServiceApiException("카카오 API 호출 중 오류가 발생했습니다", ErrorCode.EXTERNAL_API_ERROR);
		}
	}

	private MemberServiceApiException mapTokenException(WebClientResponseException e) {
		int status = e.getStatusCode().value();
		if (status == 400) {
			return new MemberServiceApiException(ErrorCode.INVALID_TOKEN_VALUE);
		}
		if (status == 401) {
			return new MemberServiceApiException("인증에 실패했습니다", ErrorCode.INVALID_TOKEN);
		}
		return new MemberServiceApiException("카카오 토큰 발급에 실패했습니다", ErrorCode.EXTERNAL_API_ERROR);
	}
}
