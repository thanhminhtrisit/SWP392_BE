package com.se1908.group01.exception;

import com.se1908.group01.dto.ApiError;
import com.se1908.group01.dto.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(
            IllegalArgumentException ex) {

        ex.printStackTrace();

        return error(
                HttpStatus.BAD_REQUEST,
                ex.getMessage(),
                fieldError(null, ex.getMessage())
        );
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingRequestParameter(
            MissingServletRequestParameterException ex) {

        ex.printStackTrace();

        return error(
                HttpStatus.BAD_REQUEST,
                "Validation failed",
                fieldError(
                        ex.getParameterName(),
                        "Required request parameter is missing"
                )
        );
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxUploadSize(
            MaxUploadSizeExceededException ex) {

        ex.printStackTrace();

        return error(
                HttpStatus.PAYLOAD_TOO_LARGE,
                "File upload failed",
                fieldError(
                        "file",
                        "File exceeds maximum upload size"
                )
        );
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiResponse<Void>> handleResponseStatus(
            ResponseStatusException ex) {

        ex.printStackTrace();

        String message = hasText(ex.getReason())
                ? ex.getReason()
                : "Request failed";

        return error(
                ex.getStatusCode(),
                message,
                fieldError(null, message)
        );
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleResourceNotFound(
            ResourceNotFoundException ex) {

        ex.printStackTrace();

        return error(
                HttpStatus.NOT_FOUND,
                ex.getMessage(),
                fieldError(null, ex.getMessage())
        );
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiResponse<Void>> handleConflict(
            ConflictException ex) {

        return error(
                HttpStatus.CONFLICT,
                ex.getMessage(),
                fieldError(null, ex.getMessage())
        );
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalState(
            IllegalStateException ex) {

        ex.printStackTrace();

        return error(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ex.getMessage(),
                fieldError(null, ex.getMessage())
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(
            MethodArgumentNotValidException ex) {

        ex.printStackTrace();

        List<ApiError> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> new ApiError(
                        error.getField(),
                        error.getDefaultMessage()
                ))
                .toList();

        return ResponseEntity
                .badRequest()
                .body(ApiResponse.error(
                        "Validation failed",
                        errors
                ));
    }

    /**
     * Bắt tất cả exception còn lại
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(
            Exception ex) {

        System.out.println("========== UNEXPECTED ERROR ==========");
        ex.printStackTrace();
        System.out.println("======================================");

        return error(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ex.getClass().getSimpleName(),
                fieldError(
                        null,
                        ex.getMessage()
                )
        );
    }

    private static ResponseEntity<ApiResponse<Void>> error(
            HttpStatusCode status,
            String message,
            ApiError error) {

        return ResponseEntity
                .status(status)
                .body(
                        ApiResponse.error(
                                message,
                                List.of(error)
                        )
                );
    }

    private static ApiError fieldError(
            String field,
            String message) {

        return new ApiError(
                field,
                hasText(message)
                        ? message
                        : "Request failed"
        );
    }

    private static boolean hasText(
            String value) {

        return value != null
                && !value.trim().isEmpty();
    }
}
