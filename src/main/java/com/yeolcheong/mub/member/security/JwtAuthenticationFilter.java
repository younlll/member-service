package com.yeolcheong.mub.member.security;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

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

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
		FilterChain filterChain) throws ServletException, IOException {

		String requestPath = request.getRequestURI();

		if (requestPath.startsWith("/actuator")) {
			log.debug("Actuator 경로 필터링 건너뜀: {}", requestPath);
			filterChain.doFilter(request, response);
			return;
		}

		String token = extractTokenFromRequest(request);

		log.debug("토큰길이: {}", token != null ? token.length() : 0);

		if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)) {
			try {
				Long memberId = jwtTokenProvider.getMemberIdFromToken(token);

				Authentication authentication = new UsernamePasswordAuthenticationToken(memberId, null, null);

				SecurityContextHolder.getContext().setAuthentication(authentication);

				log.debug("인증 성공: memberId={}", memberId);
			} catch (Exception e) {
				log.error("인증 설정 중 오류 발생: {}", e.getMessage());
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
