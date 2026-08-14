package com.se1908.group01.exception;

public class DoclingUnavailableException extends RuntimeException {

	public DoclingUnavailableException(String message) {
		super(message);
	}

	public DoclingUnavailableException(String message, Throwable cause) {
		super(message, cause);
	}
}
