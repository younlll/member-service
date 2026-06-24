package com.yeolcheong.mub.member.security;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.yeolcheong.mub.member.domain.MemberStatus;
import com.yeolcheong.mub.member.repository.MemberRepository;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private final JwtTokenProvider jwtTokenProvider;
	private final MemberRepository memberRepository;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
		FilterChain filterChain) throws ServletException, IOException {

		String requestPath = request.getRequestURI();

		if (requestPath.startsWith("/actuator")) {
			log.debug("Skipping filter for actuator path: {}", requestPath);
			filterChain.doFilter(request, response);
			return;
		}

		String token = extractTokenFromRequest(request);

		log.debug("Token length: {}", token != null ? token.length() : 0);

		if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)) {
			try {
				Long memberId = jwtTokenProvider.getMemberIdFromToken(token);

				// 토큰이 유효해도 탈퇴(DELETED)·미존재 회원은 즉시 차단한다(access token 만료 전 차단).
				if (!memberRepository.existsByIdAndStatusNot(memberId, MemberStatus.DELETED)) {
					log.warn("Authentication blocked - withdrawn or missing member: memberId={}", memberId);
				} else {
					Authentication authentication = new UsernamePasswordAuthenticationToken(memberId, null, null);

					SecurityContextHolder.getContext().setAuthentication(authentication);

					log.debug("Authentication succeeded: memberId={}", memberId);
				}
			} catch (Exception e) {
				log.error("Error while setting authentication: {}", e.getMessage());
			}
		}

		filterChain.doFilter(request, response);
	}

	private String extractTokenFromRequest(HttpServletRequest request) {
		String bearerToken = request.getHeader("Authorization");

		if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
			return bearerToken.substring(7);
		}

		return null;
	}
}
