package com.yeolcheong.mub.member.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * member_terms_agreements 테이블의
 * terms_type 컬럼 정의
 * 약관 유형
 */
@Getter
@RequiredArgsConstructor
public enum TermsType {

	TERMS_OF_SERVICE("서비스 이용약관 동의", true),
	PRIVACY_POLICY("개인정보 처리방침", true),
	LOCATION_SERVICE("위치서비스 이용약관 동의", true),
	MARKETING("마케팅 수신 동의", false);

	private final String description;
	private final boolean required;
}
