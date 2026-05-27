package com.yeolcheong.mub.member.exception;

public class OnboardingServiceApiException extends CommonException {

	public OnboardingServiceApiException(String message, ErrorCode errorCode) {
		super(message, errorCode);
	}

	public OnboardingServiceApiException(ErrorCode errorCode) {
		super(errorCode.getMessage(), errorCode);
	}
}
