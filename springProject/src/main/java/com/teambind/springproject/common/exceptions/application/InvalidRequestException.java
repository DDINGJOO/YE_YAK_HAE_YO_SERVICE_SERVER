package com.teambind.springproject.common.exceptions.application;


import com.teambind.springproject.common.exceptions.CustomException;
import com.teambind.springproject.common.exceptions.ErrorCode;

/**
 * 요청 데이터가 유효하지 않을 때 발생하는 예외
 * HTTP 400 Bad Request
 */
public class InvalidRequestException extends CustomException {

	public InvalidRequestException() {
		super(ErrorCode.INVALID_INPUT);
	}

	public InvalidRequestException(ErrorCode errorCode) {
		super(errorCode);
	}

	public InvalidRequestException(ErrorCode errorCode, String message) {
		super(errorCode, message);
	}

	@Override
	public String getExceptionType() {
		return "APPLICATION";
	}

	public static InvalidRequestException invalidFormat(String fieldName) {
		return new InvalidRequestException(
				ErrorCode.INVALID_FORMAT,
				"잘못된 형식입니다: " + fieldName
		);
	}

	public static InvalidRequestException requiredFieldMissing(String fieldName) {
		return new InvalidRequestException(
				ErrorCode.REQUIRED_FIELD_MISSING,
				"필수 필드가 누락되었습니다: " + fieldName
		);
	}

	public static InvalidRequestException valueOutOfRange(String fieldName, String range) {
		return new InvalidRequestException(
				ErrorCode.VALUE_OUT_OF_RANGE,
				fieldName + "의 값이 허용 범위를 벗어났습니다: " + range
		);
	}
}
