package com.yeolcheong.mub.member.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

	// 400 Bad Request
	INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "E40001", "입력값이 올바르지 않습니다"),
	INVALID_TYPE_VALUE(HttpStatus.BAD_REQUEST, "E40002", "타입이 올바르지 않습니다"),
	MISSING_INPUT_VALUE(HttpStatus.BAD_REQUEST, "E40003", "필수 입력값이 누락되었습니다"),
	INVALID_TOKEN_VALUE(HttpStatus.BAD_REQUEST, "E40004", "유효하지 않은 인가 코드입니다"),
	ALREADY_ONBOARDED(HttpStatus.BAD_REQUEST, "E40005", "이미 회원가입이 완료된 회원입니다"),
	TERMS_AGREEMENT_REQUIRED(HttpStatus.BAD_REQUEST, "E40006", "필수 약관에 동의해야 합니다"),
	INVALID_INTEREST_COUNT(HttpStatus.BAD_REQUEST, "E40007", "관심사는 1개 이상 3개 이하로 선택해야 합니다"),
	INTEREST_OPTION_REQUIRED(HttpStatus.BAD_REQUEST, "E40008", "관심사별 옵션은 최소 1개 이상 선택해야 합니다"),
	INVALID_INTEREST_OPTION(HttpStatus.BAD_REQUEST, "E40009", "해당 관심사에 유효하지 않은 옵션입니다"),
	INVALID_DISTRICT_CODE(HttpStatus.BAD_REQUEST, "E40010", "유효하지 않은 지역 코드입니다"),
	INVALID_IMAGE_FILE(HttpStatus.BAD_REQUEST, "E40011", "유효하지 않은 이미지 파일입니다"),
	ALREADY_WITHDRAWN(HttpStatus.BAD_REQUEST, "E40012", "이미 탈퇴한 회원입니다"),

	// 404 Not Found
	MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "E40401", "회원을 찾을 수 없습니다"),
	PROFILE_IMAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "E40403", "프로필 이미지를 찾을 수 없습니다"),

	// 409 Conflict
	DUPLICATE_NICKNAME(HttpStatus.CONFLICT, "E40901", "이미 사용 중인 닉네임입니다"),
	DUPLICATE_EMAIL(HttpStatus.CONFLICT, "E40902", "이미 사용 중인 이메일입니다"),

	// 500 Internal Server Error
	INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "E50001", "서버 내부 오류가 발생했습니다"),
	EXTERNAL_API_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "E50002", "외부 API 호출 중 오류가 발생했습니다"),
	IMAGE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "E50004", "이미지 업로드에 실패했습니다"),

	// 401 Unauthorized
	INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "E40101", "유효하지 않은 토큰입니다"),
	EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "E40102", "만료된 토큰입니다"),

	// 403 Forbidden
	ACCESS_DENIED(HttpStatus.FORBIDDEN, "E40301", "접근 권한이 없습니다");

	private final HttpStatus status;
	private final String code;
	private final String message;
}
