package com.member.exception;

import lombok.Getter;

@Getter
public class CommonException extends RuntimeException {

	private final ErrorCode errorCode;

	public CommonException(String message, ErrorCode errorCode) {
		super(message);
		this.errorCode = errorCode;
	}

	public CommonException(ErrorCode errorCode) {
		super(errorCode.getMessage());
		this.errorCode = errorCode;
	}
}
