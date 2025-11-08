package com.teambind.springproject.common.exceptions;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * PlaceInfo 도메인의 모든 예외의 기본 클래스
 * Domain Exception과 Application Exception의 공통 부모 클래스
 */
@Getter
public abstract class CustomException extends RuntimeException {

	private final ErrorCode errorCode;
	private final HttpStatus httpStatus;

	protected CustomException(ErrorCode errorCode) {
		super(errorCode.getMessage());
		this.errorCode = errorCode;
		this.httpStatus = errorCode.getStatus();
	}

	protected CustomException(ErrorCode errorCode, String message) {
		super(message);
		this.errorCode = errorCode;
		this.httpStatus = errorCode.getStatus();
	}

	protected CustomException(ErrorCode errorCode, String message, Throwable cause) {
		super(message, cause);
		this.errorCode = errorCode;
		this.httpStatus = errorCode.getStatus();
	}

	/**
	 * 예외 타입 반환 (Domain/Application 구분용)
	 */
	public abstract String getExceptionType();
}
