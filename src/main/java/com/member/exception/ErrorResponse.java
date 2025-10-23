package com.member.exception;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.validation.BindingResult;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

	private String code;
	private String message;
	private List<FieldError> errors;
	private LocalDateTime timestamp;

	private ErrorResponse(ErrorCode errorCode) {
		this.code = errorCode.getCode();
		this.message = errorCode.getMessage();
		this.timestamp = LocalDateTime.now();
	}

	private ErrorResponse(ErrorCode errorCode, String message) {
		this.code = errorCode.getCode();
		this.message = message;
		this.timestamp = LocalDateTime.now();
	}

	private ErrorResponse(ErrorCode errorCode, List<FieldError> errors) {
		this.code = errorCode.getCode();
		this.message = errorCode.getMessage();
		this.errors = errors;
		this.timestamp = LocalDateTime.now();
	}

	public static ErrorResponse of(ErrorCode errorCode) {
		return new ErrorResponse(errorCode);
	}

	public static ErrorResponse of(ErrorCode errorCode, String message) {
		return new ErrorResponse(errorCode, message);
	}

	public static ErrorResponse of(ErrorCode errorCode, BindingResult bindingResult) {
		return new ErrorResponse(errorCode, FieldError.of(bindingResult));
	}

	@Getter
	@NoArgsConstructor(access = AccessLevel.PROTECTED)
	public static class FieldError {
		private String field;
		private String value;
		private String reason;

		private FieldError(String field, String value, String reason) {
			this.field = field;
			this.value = value;
			this.reason = reason;
		}

		public static List<FieldError> of(BindingResult bindingResult) {
			List<org.springframework.validation.FieldError> fieldErrors = bindingResult.getFieldErrors();
			List<FieldError> errors = new ArrayList<>();

			for (org.springframework.validation.FieldError error : fieldErrors) {
				errors.add(new FieldError(
					error.getField(),
					error.getRejectedValue() == null ? "" : error.getRejectedValue().toString(),
					error.getDefaultMessage()
				));
			}

			return errors;
		}
	}
}
