package com.teambind.springproject.common.exceptions;


import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {


	@ExceptionHandler(CustomException.class)
	public ResponseEntity<ErrorResponse> handlePlaceException(
			CustomException ex, HttpServletRequest request) {
		log.warn("PlaceException [{}]: {}", ex.getExceptionType(), ex.getMessage());

		ErrorResponse errorResponse = ErrorResponse.of(ex, request.getRequestURI());
		return ResponseEntity.status(ex.getHttpStatus()).body(errorResponse);
	}

	


	/**
	 * Validation 예외 처리 (필드 에러 상세 정보 포함)
	 */
	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(
			MethodArgumentNotValidException ex,
			HttpHeaders headers,
			org.springframework.http.HttpStatusCode status,
			WebRequest request) {
		log.warn("Validation failed: {} field errors", ex.getBindingResult().getFieldErrorCount());

		ErrorResponse body = ErrorResponse.ofValidation(
				HttpStatus.BAD_REQUEST.value(),
				"VALIDATION_ERROR",
				"입력값 검증에 실패했습니다.",
				extractPath(request),
				ex.getBindingResult().getFieldErrors()
		);
		return ResponseEntity.badRequest().body(body);
	}

	/**
	 * 일반 예외 처리 (최종 fallback)
	 */
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleGeneralException(
			Exception ex, HttpServletRequest request) {
		log.error("Unexpected exception occurred", ex);

		ErrorResponse errorResponse = ErrorResponse.of(
				HttpStatus.INTERNAL_SERVER_ERROR.value(),
				ErrorCode.INTERNAL_SERVER_ERROR.getErrCode(),
				"서버 내부 오류가 발생했습니다.",
				request.getRequestURI()
		);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
	}

	private String extractPath(WebRequest request) {
		String description = request.getDescription(false);
		return description.replace("uri=", "");
	}
}
