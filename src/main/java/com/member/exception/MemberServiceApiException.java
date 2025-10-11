package com.member.exception;

public class MemberServiceApiException extends RuntimeException {
	public MemberServiceApiException(String message) {
		super(message);
	}
}
