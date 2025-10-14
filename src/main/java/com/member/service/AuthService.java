package com.member.service;

import java.util.Optional;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.member.common.KakaoProperties;
import com.member.common.SnsProvider;
import com.member.domain.Member;
import com.member.dto.LoginResponse;
import com.member.dto.LoginTokenResponse;
import com.member.dto.SnsUserInfoResponse;
import com.member.exception.MemberServiceApiException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class AuthService {

    private final KakaoProperties kakaoProperties;
    private final WebClient webClient;

    private final MemberService memberService;

    public String getAuthUrl() {
        return kakaoProperties.getAuthUrl() +
                "?client_id=" + kakaoProperties.getClientId() +
                "&redirect_uri=" + kakaoProperties.getRedirectUri() +
                "&response_type=code";
    }

	@Transactional
    public LoginResponse login(String code) {
        log.info("카카오 로그인 시작");

        boolean isNewMember = false;

        LoginTokenResponse loginTokenResponse = getAccessToken(code);
        SnsUserInfoResponse snsUserInfoResponse = getKakaoUserInfo(loginTokenResponse);
        Optional<Member> existingMemberInformation = memberService.findBySocialId(SnsProvider.KAKAO,
            snsUserInfoResponse.getKakaoIdAsString()
        );

        Member member;
        if (existingMemberInformation.isEmpty()) {
            isNewMember = true;
            member = memberService.createdFromSnsUser(snsUserInfoResponse);
        } else {
            member = existingMemberInformation.get();
        }

		return LoginResponse.of(
            loginTokenResponse,
            snsUserInfoResponse,
            isNewMember,
			String.valueOf(member.getId())
        );
    }

    private SnsUserInfoResponse getKakaoUserInfo(LoginTokenResponse loginTokenResponse) {
        log.info("카카오 사용자 정보 조회 시작");

        try {
            SnsUserInfoResponse snsUserInfoResponse = webClient.get()
                .uri(kakaoProperties.getUserInfoUrl())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + loginTokenResponse.getAccessToken())
                .retrieve()
                .bodyToMono(SnsUserInfoResponse.class)
                .block();

            if (snsUserInfoResponse == null) {
                throw new MemberServiceApiException("카카오 사용자 정보 응답이 없습니다.");
            }

            log.info("카카오 사용자 정보 조회 성공: kakaoId={}, email={}", snsUserInfoResponse.getId(), snsUserInfoResponse.getKakaoAccount().getEmail());

            return snsUserInfoResponse;
        } catch (WebClientResponseException e) {
            log.error("카카오 사용자 정보 조회 실패: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new MemberServiceApiException("카카오 사용자 정보 조회에 실패했습니다: " + e.getMessage());
        }
    }

    private LoginTokenResponse getAccessToken(String code) {
        log.info("로그인을 위한 Access Token 요청 시작: code={}", code);

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
