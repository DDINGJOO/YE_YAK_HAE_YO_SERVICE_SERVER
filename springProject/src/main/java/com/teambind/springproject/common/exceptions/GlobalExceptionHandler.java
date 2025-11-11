package com.teambind.springproject.common.exceptions;


import com.teambind.springproject.domain.pricingpolicy.exception.PricingPolicyException;
import com.teambind.springproject.domain.reservationpricing.exception.ReservationPricingException;
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


	@ExceptionHandler(PricingPolicyException.class)
	public ResponseEntity<ErrorResponse> handlePricingPolicyException(
			PricingPolicyException ex, HttpServletRequest request) {
		log.warn("PricingPolicyException [{}]: {}", ex.getExceptionType(), ex.getMessage());

		ErrorResponse errorResponse = ErrorResponse.builder()
				.timestamp(java.time.LocalDateTime.now())
				.status(ex.getHttpStatus().value())
				.code(ex.getErrorCode().getErrCode())
				.message(ex.getMessage())
				.path(request.getRequestURI())
				.exceptionType(ex.getExceptionType())
				.build();
		return ResponseEntity.status(ex.getHttpStatus()).body(errorResponse);
	}

	@ExceptionHandler(ReservationPricingException.class)
	public ResponseEntity<ErrorResponse> handleReservationPricingException(
			ReservationPricingException ex, HttpServletRequest request) {
		log.warn("ReservationPricingException [{}]: {}", ex.getExceptionType(), ex.getMessage());

		ErrorResponse errorResponse = ErrorResponse.builder()
				.timestamp(java.time.LocalDateTime.now())
				.status(ex.getHttpStatus().value())
				.code(ex.getErrorCode().getErrCode())
				.message(ex.getMessage())
				.path(request.getRequestURI())
				.exceptionType(ex.getExceptionType())
				.build();
		return ResponseEntity.status(ex.getHttpStatus()).body(errorResponse);
	}

	/**
	 * NoSuchElementException 처리 (리소스 없음)
	 */
	@ExceptionHandler(java.util.NoSuchElementException.class)
	public ResponseEntity<ErrorResponse> handleNoSuchElementException(
			java.util.NoSuchElementException ex, HttpServletRequest request) {
		log.warn("Resource not found: {}", ex.getMessage());

		ErrorResponse errorResponse = ErrorResponse.of(
				HttpStatus.NOT_FOUND.value(),
				"RESOURCE_NOT_FOUND",
				ex.getMessage() != null ? ex.getMessage() : "요청한 리소스를 찾을 수 없습니다.",
				request.getRequestURI()
		);
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
	}

	/**
	 * ConstraintViolationException 처리 (@PathVariable, @RequestParam validation 실패)
	 */
	@ExceptionHandler(jakarta.validation.ConstraintViolationException.class)
	public ResponseEntity<ErrorResponse> handleConstraintViolationException(
			jakarta.validation.ConstraintViolationException ex, HttpServletRequest request) {
		log.warn("Constraint violation: {}", ex.getMessage());

		ErrorResponse errorResponse = ErrorResponse.of(
				HttpStatus.BAD_REQUEST.value(),
				"CONSTRAINT_VIOLATION",
				ex.getMessage(),
				request.getRequestURI()
		);
		return ResponseEntity.badRequest().body(errorResponse);
	}

	/**
	 * IllegalArgumentException 처리 (도메인 validation 실패)
	 */
	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
			IllegalArgumentException ex, HttpServletRequest request) {
		log.warn("Invalid argument: {}", ex.getMessage());

		ErrorResponse errorResponse = ErrorResponse.of(
				HttpStatus.BAD_REQUEST.value(),
				"INVALID_ARGUMENT",
				ex.getMessage(),
				request.getRequestURI()
		);
		return ResponseEntity.badRequest().body(errorResponse);
	}

	/**
	 * IllegalStateException 처리 (도메인 상태 위반)
	 */
	@ExceptionHandler(IllegalStateException.class)
	public ResponseEntity<ErrorResponse> handleIllegalStateException(
			IllegalStateException ex, HttpServletRequest request) {
		log.warn("Invalid state: {}", ex.getMessage());

		ErrorResponse errorResponse = ErrorResponse.of(
				HttpStatus.CONFLICT.value(),
				"INVALID_STATE",
				ex.getMessage(),
				request.getRequestURI()
		);
		return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
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
				"INTERNAL_SERVER_ERROR",
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
