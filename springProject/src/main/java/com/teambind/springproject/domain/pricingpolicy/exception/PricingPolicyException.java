package com.teambind.springproject.domain.pricingpolicy.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 가격 정책 도메인 예외 기본 클래스.
 */
@Getter
public abstract class PricingPolicyException extends RuntimeException {
	
	private final PricingPolicyErrorCode errorCode;
	private final HttpStatus httpStatus;
	
	protected PricingPolicyException(final PricingPolicyErrorCode errorCode) {
		super(errorCode.getMessage());
		this.errorCode = errorCode;
		this.httpStatus = errorCode.getStatus();
	}
	
	protected PricingPolicyException(final PricingPolicyErrorCode errorCode, final String message) {
		super(message);
		this.errorCode = errorCode;
		this.httpStatus = errorCode.getStatus();
	}
	
	protected PricingPolicyException(final PricingPolicyErrorCode errorCode, final String message,
	                                 final Throwable cause) {
		super(message, cause);
		this.errorCode = errorCode;
		this.httpStatus = errorCode.getStatus();
	}
	
	public abstract String getExceptionType();
}
