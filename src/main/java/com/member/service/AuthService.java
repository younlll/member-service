package com.member.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.member.dto.LoginTokenResponse;
import com.member.exception.MemberServiceApiException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class AuthService {

    private final WebClient webClient;

    @Value("${kakao.auth-url}")
    private String authUrl;

    @Value("${kakao.client-id}")
    private String clientId;

    @Value("${kakao.redirect-uri}")
    private String redirectUri;

    @Value("${kakao.token-url}")
    private String tokenUrl;

    public String getAuthUrl() {
        return authUrl +
                "?client_id=" + clientId +
                "&redirect_uri=" + redirectUri +
                "&response_type=code";
    }

	@Transactional
    public LoginTokenResponse login(String code) {
        log.info("카카오 로그인 시작");

		return getAccessToken(code);
    }

    private LoginTokenResponse getAccessToken(String code) {
        log.info("로그인을 위한 Access Token 요청 시작: code={}", code);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", clientId);
        params.add("redirect_uri", redirectUri);
        params.add("code", code);

        try {
            LoginTokenResponse response = webClient.post()
                .uri(tokenUrl)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue(params)
                .retrieve()
                .bodyToMono(LoginTokenResponse.class)
                .block();

            if (response == null) {
                throw new MemberServiceApiException("토큰 응답이 없습니다.");
            }

            log.info("로그인 Access Token 발급 성공");
            return response;
        } catch (WebClientResponseException e) {
            log.error("Access Token 발급 실패: status={}, body={}",
                e.getStatusCode(), e.getResponseBodyAsString());
            throw new MemberServiceApiException("토큰 발급에 실패했습니다: " + e.getMessage());
        }
    }
}
