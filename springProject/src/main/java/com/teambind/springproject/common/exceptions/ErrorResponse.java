package com.teambind.springproject.common.exceptions;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;
import org.springframework.validation.FieldError;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
	private LocalDateTime timestamp;

	private int status;
	private String code;
	private String message;
	private String path;
	private String exceptionType;

	@Builder.Default
	private List<FieldErrorDetail> fieldErrors = new ArrayList<>();

	public static ErrorResponse of(int status, String code, String message, String path) {
		return ErrorResponse.builder()
				.timestamp(LocalDateTime.now())
				.status(status)
				.code(code)
				.message(message)
				.path(path)
				.build();
	}

	public static ErrorResponse of(CustomException ex, String path) {
		return ErrorResponse.builder()
				.timestamp(LocalDateTime.now())
				.status(ex.getHttpStatus().value())
				.code(ex.getErrorCode().getErrCode())
				.message(ex.getMessage())
				.path(path)
				.exceptionType(ex.getExceptionType())
				.build();
	}

	public static ErrorResponse ofValidation(int status, String code, String message, String path, List<FieldError> errors) {
		List<FieldErrorDetail> fieldErrors = errors.stream()
				.map(error -> new FieldErrorDetail(
						error.getField(),
						error.getRejectedValue() != null ? error.getRejectedValue().toString() : null,
						error.getDefaultMessage()
				))
				.toList();

		return ErrorResponse.builder()
				.timestamp(LocalDateTime.now())
				.status(status)
				.code(code)
				.message(message)
				.path(path)
				.fieldErrors(fieldErrors)
				.build();
	}

	@Getter
	@Builder
	public static class FieldErrorDetail {
		private String field;
		private String rejectedValue;
		private String message;

		public FieldErrorDetail(String field, String rejectedValue, String message) {
			this.field = field;
			this.rejectedValue = rejectedValue;
			this.message = message;
		}
	}
}
