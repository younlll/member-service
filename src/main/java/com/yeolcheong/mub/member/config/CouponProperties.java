package com.yeolcheong.mub.member.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

/**
 * 쿠폰 발급 관련 설정.
 * 회원가입(온보딩) 완료 시 발급할 활동이용권 정책 식별자를 보관한다.
 */
@Component
@ConfigurationProperties(prefix = "coupon.welcome-voucher")
@Getter
@Setter
public class CouponProperties {

	// 가입 축하 활동이용권으로 사용할 coupon_policies.id
	private Long policyId;
}
