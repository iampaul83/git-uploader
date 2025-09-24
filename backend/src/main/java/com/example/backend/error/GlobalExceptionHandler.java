package com.example.backend.error;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

        @ExceptionHandler(RepoNotFoundException.class)
        public ResponseEntity<Map<String, Object>> handleRepoNotFound(RepoNotFoundException exception) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .body(createBody(HttpStatus.NOT_FOUND, exception.getMessage()));
        }

        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException exception) {
                Map<String, Object> body = createBody(HttpStatus.BAD_REQUEST, "Validation failed");
                Map<String, String> errors = exception.getBindingResult()
                                .getFieldErrors()
                                .stream()
                                .collect(Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage, (first, second) -> first,
                                                LinkedHashMap::new));
                body.put("errors", errors);
                return ResponseEntity.badRequest().body(body);
        }

        @ExceptionHandler(ConstraintViolationException.class)
        public ResponseEntity<Map<String, Object>> handleConstraintViolation(ConstraintViolationException exception) {
                Map<String, Object> body = createBody(HttpStatus.BAD_REQUEST, "Validation failed");
                body.put("errors", exception.getConstraintViolations()
                                .stream()
                                .collect(Collectors.toMap(violation -> violation.getPropertyPath().toString(),
                                                violation -> violation.getMessage(), (first, second) -> first,
                                                LinkedHashMap::new)));
                return ResponseEntity.badRequest().body(body);
        }

        @ExceptionHandler(InvalidRequestException.class)
        public ResponseEntity<Map<String, Object>> handleInvalidRequest(InvalidRequestException exception) {
                return ResponseEntity.badRequest()
                                .body(createBody(HttpStatus.BAD_REQUEST, exception.getMessage()));
        }

        @ExceptionHandler(GitOperationException.class)
        public ResponseEntity<Map<String, Object>> handleGitFailure(GitOperationException exception) {
                Map<String, Object> body = createBody(HttpStatus.INTERNAL_SERVER_ERROR, exception.getMessage());
                Map<String, String> details = new LinkedHashMap<>();
                details.put("command", exception.getCommand());
                if (exception.getStdout() != null && !exception.getStdout().isBlank()) {
                        details.put("stdout", exception.getStdout());
                }
                if (exception.getStderr() != null && !exception.getStderr().isBlank()) {
                        details.put("stderr", exception.getStderr());
                }
                body.put("details", details);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
        }

        @ExceptionHandler(Exception.class)
        public ResponseEntity<Map<String, Object>> handleUnexpected(Exception exception) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(createBody(HttpStatus.INTERNAL_SERVER_ERROR, exception.getMessage()));
        }

        private Map<String, Object> createBody(HttpStatus status, String message) {
                Map<String, Object> body = new LinkedHashMap<>();
                body.put("timestamp", Instant.now().toString());
                body.put("status", status.value());
                body.put("error", status.getReasonPhrase());
                body.put("message", message);
                return body;
        }
}
