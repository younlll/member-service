package com.member.exception;

public class MemberServiceApiException extends CommonException {
	public MemberServiceApiException(String message, ErrorCode errorCode) {
		super(message, errorCode);
	}

	public MemberServiceApiException(ErrorCode errorCode) {
		super(errorCode.getMessage(), errorCode);
	}

	public MemberServiceApiException(String message) {
		super(message, ErrorCode.EXTERNAL_API_ERROR);
	}

	public MemberServiceApiException() {
		super(ErrorCode.EXTERNAL_API_ERROR);
	}
}
