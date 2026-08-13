package com.se1908.group01.dto;

import java.time.Instant;
import java.util.List;

public class ApiResponse<T> {

	private boolean success;
	private String message;
	private T data;
	private List<ApiError> errors;
	private Instant timestamp;

	public ApiResponse() {
	}

	private ApiResponse(boolean success, String message, T data, List<ApiError> errors) {
		this.success = success;
		this.message = message;
		this.data = data;
		this.errors = errors;
		this.timestamp = Instant.now();
	}

	public static <T> ApiResponse<T> success(String message, T data) {
		return new ApiResponse<>(true, message, data, null);
	}

	public static <T> ApiResponse<T> error(String message, List<ApiError> errors) {
		return new ApiResponse<>(false, message, null, errors);
	}

	public boolean isSuccess() {
		return success;
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public T getData() {
		return data;
	}

	public void setData(T data) {
		this.data = data;
	}

	public List<ApiError> getErrors() {
		return errors;
	}

	public void setErrors(List<ApiError> errors) {
		this.errors = errors;
	}

	public Instant getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Instant timestamp) {
		this.timestamp = timestamp;
	}
}

