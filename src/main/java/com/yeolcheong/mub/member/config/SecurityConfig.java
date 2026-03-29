package com.yeolcheong.mub.member.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.yeolcheong.mub.member.security.JwtAuthenticationFilter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

	private final JwtAuthenticationFilter jwtAuthenticationFilter;

	/**
	 * Configure security to ignore requests for static resources at common locations and actuator endpoints.
	 *
	 * @return the WebSecurityCustomizer that excludes static resources and requests under `/actuator/**` from security filters
	 */
	@Bean
	public WebSecurityCustomizer webSecurityCustomizer() {
		return web -> web.ignoring()
			.requestMatchers(PathRequest.toStaticResources().atCommonLocations())
			.requestMatchers("/actuator/**");
	}

	/**
	 * Create the application's SecurityFilterChain configured for JWT-based authentication.
	 *
	 * Configures CSRF as disabled, enables CORS with the application's CorsConfigurationSource, uses
	 * stateless session management, permits unauthenticated access to "/api/auth/**", "/api/districts/**",
	 * and "/api/interests", requires authentication for other requests, and inserts the JWT authentication
	 * filter before the UsernamePasswordAuthenticationFilter.
	 *
	 * @param httpSecurity the HttpSecurity to configure
	 * @return the configured SecurityFilterChain
	 * @throws Exception if an error occurs while building the security configuration
	 */
	@Bean
	public SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception {
		httpSecurity.csrf(AbstractHttpConfigurer::disable)
			.cors(cors -> cors.configurationSource(corsConfigurationSource()))
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.authorizeHttpRequests(auth -> auth
				.requestMatchers(
					"/api/auth/**",
					"/api/districts/**",
					"/api/interests"
				).permitAll()
				.anyRequest().authenticated()
			)
			.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

		return httpSecurity.build();
	}

	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration configuration = new CorsConfiguration();

		configuration.setAllowedOrigins(Arrays.asList(
			"http://localhost:3000",
			"capacitor://localhost",
			"ionic://localhost"
		));

		configuration.setAllowedMethods(Arrays.asList(
			"GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
		));

		configuration.setAllowedHeaders(List.of("*"));
		configuration.setAllowCredentials(true);
		configuration.setExposedHeaders(Arrays.asList(
			"Authorization", "Refresh-Token"
		));

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);

		return source;
	}
}
