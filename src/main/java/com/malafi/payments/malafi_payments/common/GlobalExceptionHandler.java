package com.malafi.payments.malafi_payments.common;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatusException(
            ResponseStatusException exception,
            HttpServletRequest request) {
        HttpStatusCode statusCode = exception.getStatusCode();
        String message = exception.getReason() != null ? exception.getReason() : "Request failed";

        return ResponseEntity
                .status(statusCode)
                .body(toErrorResponse(statusCode, message, request));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException exception,
            HttpServletRequest request) {
        String message = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));

        return ResponseEntity
                .badRequest()
                .body(toErrorResponse(HttpStatus.BAD_REQUEST, message, request));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalStateException(
            IllegalStateException exception,
            HttpServletRequest request) {
        return ResponseEntity
                .badRequest()
                .body(toErrorResponse(HttpStatus.BAD_REQUEST, exception.getMessage(), request));
    }

    private ErrorResponse toErrorResponse(
            HttpStatusCode statusCode,
            String message,
            HttpServletRequest request) {
        HttpStatus httpStatus = HttpStatus.resolve(statusCode.value());
        String error = httpStatus != null ? httpStatus.getReasonPhrase() : "HTTP " + statusCode.value();

        return new ErrorResponse(
                Instant.now(),
                statusCode.value(),
                error,
                message,
                request.getRequestURI()
        );
    }
}
