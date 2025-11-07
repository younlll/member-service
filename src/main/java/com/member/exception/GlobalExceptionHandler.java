package com.member.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import lombok.extern.slf4j.Slf4j;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

	@ExceptionHandler(MethodArgumentNotValidException.class)
	protected ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
		System.out.println("========== MethodArgumentNotValidException 호출됨! ==========");
		log.error("MethodArgumentNotValidException", ex);
		ErrorResponse response = ErrorResponse.of(ErrorCode.INVALID_INPUT_VALUE, ex.getBindingResult());

		System.out.println("ErrorResponse: " + response);
		System.out.println("ErrorResponse code: " + response.getCode());
		System.out.println("ErrorResponse message: " + response.getMessage());

		return ResponseEntity.status(ErrorCode.INVALID_INPUT_VALUE.getStatus()).body(response);
	}

	@ExceptionHandler(BindException.class)
	public ResponseEntity<ErrorResponse> handleBindException(BindException ex) {
		System.out.println("========== BindException 호출됨! ==========");
		log.error("BindException", ex);
		ErrorResponse response = ErrorResponse.of(ErrorCode.INVALID_INPUT_VALUE, ex.getBindingResult());

		System.out.println("ErrorResponse: " + response);
		System.out.println("ErrorResponse code: " + response.getCode());
		System.out.println("ErrorResponse message: " + response.getMessage());

		return ResponseEntity.status(ErrorCode.INVALID_INPUT_VALUE.getStatus()).body(response);
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatchException(
		MethodArgumentTypeMismatchException ex) {
		System.out.println("========== MethodArgumentTypeMismatchException 호출됨! ==========");
		log.error("MethodArgumentTypeMismatchException", ex);
		ErrorResponse response = ErrorResponse.of(ErrorCode.INVALID_TYPE_VALUE);

		System.out.println("ErrorResponse: " + response);
		System.out.println("ErrorResponse code: " + response.getCode());
		System.out.println("ErrorResponse message: " + response.getMessage());

		return ResponseEntity.status(ErrorCode.INVALID_TYPE_VALUE.getStatus()).body(response);
	}

	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	public ResponseEntity<ErrorResponse> handleHttpRequestMethodNotSupportedException(
		HttpRequestMethodNotSupportedException ex) {
		System.out.println("========== HttpRequestMethodNotSupportedException 호출됨! ==========");
		log.error("HttpRequestMethodNotSupportedException", ex);
		ErrorResponse response = ErrorResponse.of(ErrorCode.INVALID_INPUT_VALUE);

		System.out.println("ErrorResponse: " + response);
		System.out.println("ErrorResponse code: " + response.getCode());
		System.out.println("ErrorResponse message: " + response.getMessage());

		return ResponseEntity.status(ErrorCode.INVALID_INPUT_VALUE.getStatus()).body(response);
	}

	@ExceptionHandler(NoResourceFoundException.class)
	public ResponseEntity<ErrorResponse> handleNoResourceFoundException(NoResourceFoundException ex) {
		String requestPath = ex.getResourcePath();
		log.debug("요청 경로: {}", requestPath);

		if (requestPath.startsWith("actuator")) {
			log.info("Actuator 요청 무시: {}", requestPath);
			return null;
		}

		log.error("NoResourceFoundException 발생: {}", requestPath);
		ErrorResponse response = ErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
	}

	@ExceptionHandler(CommonException.class)
	protected ResponseEntity<ErrorResponse> handleCommonException(CommonException ex) {
		System.out.println("========== CommonException 호출됨! ==========");
		log.error("CommonException: {}", ex.getMessage(), ex);
		ErrorCode errorCode = ex.getErrorCode();
		ErrorResponse response = ErrorResponse.of(errorCode, ex.getMessage());

		System.out.println("ErrorResponse: " + response);
		System.out.println("ErrorResponse code: " + response.getCode());
		System.out.println("ErrorResponse message: " + response.getMessage());

		return ResponseEntity.status(errorCode.getStatus()).body(response);
	}

	@ExceptionHandler(MemberServiceApiException.class)
	protected ResponseEntity<ErrorResponse> handleMemberServiceApiException(MemberServiceApiException ex) {
		System.out.println("========== MemberServiceApiException 호출됨! ==========");
		log.error("MemberServiceApiException: {}", ex.getMessage(), ex);
		ErrorCode errorCode = ex.getErrorCode();
		ErrorResponse response = ErrorResponse.of(errorCode, ex.getMessage());

		System.out.println("ErrorResponse: " + response);
		System.out.println("ErrorResponse code: " + response.getCode());
		System.out.println("ErrorResponse message: " + response.getMessage());

		return ResponseEntity.status(errorCode.getStatus()).body(response);
	}

	@ExceptionHandler(Exception.class)
	protected ResponseEntity<ErrorResponse> handleException(Exception ex) {
		System.out.println("========== Exception 호출됨! ==========");
		log.error("Exception: {}", ex.getMessage(), ex);
		ErrorResponse response = ErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR);

		System.out.println("ErrorResponse: " + response);
		System.out.println("ErrorResponse code: " + response.getCode());
		System.out.println("ErrorResponse message: " + response.getMessage());

		return ResponseEntity.status(ErrorCode.INTERNAL_SERVER_ERROR.getStatus()).body(response);
	}
}
