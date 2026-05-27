package com.yeolcheong.mub.member.domain;

public enum MemberStatus {
	// Mub구독
	MUBACTIVE,

	// 회원가입완료
	ACTIVE,

	// 회원가입완료이전 회원, 휴면회원
	INACTIVE,

	// 탈퇴회원
	DELETED
}
