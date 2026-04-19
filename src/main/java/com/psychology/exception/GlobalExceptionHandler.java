package com.psychology.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.WebRequest;

import jakarta.validation.ConstraintViolationException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Object> handleValidationExceptions(MethodArgumentNotValidException ex, WebRequest request) {
        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(this::formatFieldError)
                .orElse("Validation failed");
        return new ResponseEntity<>(buildBody(HttpStatus.BAD_REQUEST, message, request, ex), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Object> handleConstraintViolation(ConstraintViolationException ex, WebRequest request) {
        String message = ex.getConstraintViolations()
                .stream()
                .findFirst()
                .map(v -> v.getMessage())
                .orElse("Validation failed");
        return new ResponseEntity<>(buildBody(HttpStatus.BAD_REQUEST, message, request, ex), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Object> handleNotReadable(HttpMessageNotReadableException ex, WebRequest request) {
        return new ResponseEntity<>(
                buildBody(HttpStatus.BAD_REQUEST, "Malformed request body", request, ex),
                HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleAllExceptions(Exception ex, WebRequest request) {
        log.error("Unhandled exception: ", ex);
        return new ResponseEntity<>(
                buildBody(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage(), request, ex),
                HttpStatus.INTERNAL_SERVER_ERROR
        );
    }

    private String formatFieldError(FieldError error) {
        if (error.getDefaultMessage() == null || error.getDefaultMessage().isBlank()) {
            return "Validation failed";
        }
        return error.getDefaultMessage();
    }

    private Map<String, Object> buildBody(HttpStatus status, String message, WebRequest request, Exception ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        body.put("path", request.getDescription(false));

        if (ex.getStackTrace() != null && ex.getStackTrace().length > 0) {
            StackTraceElement first = ex.getStackTrace()[0];
            body.put("location", first.getClassName() + "." + first.getMethodName() + ":" + first.getLineNumber());
        }
        return body;
    }
}
