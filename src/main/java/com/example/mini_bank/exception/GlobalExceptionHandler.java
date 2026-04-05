package com.example.mini_bank.exception;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.MethodArgumentNotValidException; // for @Valid
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.naming.AuthenticationException;
import javax.persistence.EntityNotFoundException;
import javax.validation.ConstraintViolationException;
import javax.validation.ValidationException;

import org.springframework.validation.FieldError;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private final Logger log = LoggerFactory.getLogger(getClass());

 // Custom validation exceptions from ValidationService
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(ValidationException ex) {
        log.warn("Custom validation error: {}", ex.getMessage(), ex);

        ErrorResponse response = new ErrorResponse(
                "Validation error: " + ex.getMessage(),
                HttpStatus.BAD_REQUEST.value(),
                LocalDateTime.now(),
                null
        );

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }
    
    //  @RequestParam & @PathVariable
    @ExceptionHandler({ConstraintViolationException.class, javax.validation.ConstraintViolationException.class})
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(Exception ex) {
        
        log.warn("Constraint violation: {}", ex.getMessage(), ex);

        Map<String, String> errors = new HashMap<>();

        if (ex instanceof ConstraintViolationException springEx) {
            springEx.getConstraintViolations().forEach(violation -> {
                String fieldName = violation.getPropertyPath().toString();
                String errorMessage = violation.getMessage();
                errors.put(fieldName, errorMessage);
            });
        } else if (ex instanceof javax.validation.ConstraintViolationException javaxEx) {
            javaxEx.getConstraintViolations().forEach(violation -> {
                String fieldName = violation.getPropertyPath().toString();
                String errorMessage = violation.getMessage();
                errors.put(fieldName, errorMessage);
            });
        }

        ErrorResponse response = new ErrorResponse(
                "Validation error: invalid request parameters",
                HttpStatus.BAD_REQUEST.value(),
                LocalDateTime.now(),
                errors
        );

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    
    // @Valid DTO validation errors
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleDtoValidation(MethodArgumentNotValidException ex) {
        log.warn("DTO validation error: {}", ex.getMessage(), ex);

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        String firstMessage = errors.values().stream().findFirst().orElse("Validation error");

        ErrorResponse response = new ErrorResponse(
                firstMessage,
                HttpStatus.BAD_REQUEST.value(),
                LocalDateTime.now(),
                errors
        );

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.warn("Illegal argument: {}", ex.getMessage(), ex);

        ErrorResponse response = new ErrorResponse(
                "Validation error: " + ex.getMessage(),
                HttpStatus.BAD_REQUEST.value(),
                LocalDateTime.now(),
                null
        );

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }
    
    // Entity not found
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEntityNotFound(EntityNotFoundException ex) {
        log.warn("Entity not found: {}", ex.getMessage(), ex);

        ErrorResponse response = new ErrorResponse(
                "Not found error: " + ex.getMessage(),
                HttpStatus.NOT_FOUND.value(),
                LocalDateTime.now(),
                null
        );

        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }
    
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ErrorResponse> handleNoSuchElement(NoSuchElementException ex) {
        log.warn("Entity not found: {}", ex.getMessage(), ex);
        ErrorResponse response = new ErrorResponse(
                "Not found error: " + ex.getMessage(),
                HttpStatus.NOT_FOUND.value(),
                LocalDateTime.now(),
                null
        );
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    // Custom authentication exceptions
    @ExceptionHandler(AuthException.class)
    public ResponseEntity<ErrorResponse> handleAuthException(AuthException ex) {
        log.warn("Authentication error: {}", ex.getMessage(), ex);

        ErrorResponse response = new ErrorResponse(
                "Authentication error: " + ex.getMessage(),
                ex.getStatus().value(),
                LocalDateTime.now(),
                null
        );

        return new ResponseEntity<>(response, ex.getStatus());
    }
    
    @ExceptionHandler(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatchException(
            org.springframework.web.method.annotation.MethodArgumentTypeMismatchException ex) {
        
        log.warn("Method argument type mismatch: {}", ex.getMessage(), ex);

        String errorMessage = "Invalid parameter value: " + ex.getValue();
        if (ex.getRequiredType() != null && ex.getRequiredType().isEnum()) {
            errorMessage += ". Available values: " + Arrays.toString(ex.getRequiredType().getEnumConstants());
        }

        ErrorResponse response = new ErrorResponse(
                errorMessage,
                HttpStatus.BAD_REQUEST.value(),
                LocalDateTime.now(),
                null
        );

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }
    
    @ExceptionHandler(org.springframework.core.convert.ConversionFailedException.class)
    public ResponseEntity<ErrorResponse> handleConversionFailedException(org.springframework.core.convert.ConversionFailedException ex) {
        log.warn("Conversion error: {}", ex.getMessage(), ex);

        ErrorResponse response = new ErrorResponse(
                "Validation error: invalid enum value - " + ex.getValue(),
                HttpStatus.BAD_REQUEST.value(),
                LocalDateTime.now(),
                null
        );

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(org.springframework.beans.ConversionNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleConversionNotSupportedException(org.springframework.beans.ConversionNotSupportedException ex) {
        log.warn("Conversion not supported: {}", ex.getMessage(), ex);

        ErrorResponse response = new ErrorResponse(
                "Validation error: invalid parameter value",
                HttpStatus.BAD_REQUEST.value(),
                LocalDateTime.now(),
                null
        );

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex) {
        log.warn("Bad credentials: {}", ex.getMessage(), ex);

        ErrorResponse response = new ErrorResponse(
                "Authentication error: invalid email or password",
                HttpStatus.UNAUTHORIZED.value(),
                LocalDateTime.now(),
                null
        );

        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(AuthenticationException ex) {
        log.warn("Authentication exception: {}", ex.getMessage(), ex);

        ErrorResponse response = new ErrorResponse(
                "Authentication error: " + ex.getMessage(),
                HttpStatus.UNAUTHORIZED.value(),
                LocalDateTime.now(),
                null
        );

        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
    }

    // Security exceptions
    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<ErrorResponse> handleSecurityException(SecurityException ex) {
        log.warn("Security exception: {}", ex.getMessage(), ex);

        ErrorResponse response = new ErrorResponse(
                "Security error: " + ex.getMessage(),
                HttpStatus.FORBIDDEN.value(),
                LocalDateTime.now(),
                null
        );

        return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
    }

    // Illegal state or argument
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalStateException(IllegalStateException ex) {
        log.warn("Illegal state: {}", ex.getMessage(), ex);

        if (ex.getMessage().contains("Insufficient funds")) {
            ErrorResponse response = new ErrorResponse(
                    "Insufficient funds: " + ex.getMessage(),
                    HttpStatus.BAD_REQUEST.value(),
                    LocalDateTime.now(),
                    null
            );
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        ErrorResponse response = new ErrorResponse(
                "Invalid operation: " + ex.getMessage(),
                HttpStatus.BAD_REQUEST.value(),
                LocalDateTime.now(),
                null
        );

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }


    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ErrorResponse> handleDataAccessException(DataAccessException ex) {
        log.error("Database error: {}", ex.getMessage(), ex);

        Throwable root = ex.getRootCause();
        if (root instanceof IllegalStateException ise && ise.getMessage().contains("Insufficient funds")) {
            ErrorResponse response = new ErrorResponse(
                    "Insufficient funds: " + ise.getMessage(),
                    HttpStatus.BAD_REQUEST.value(),
                    LocalDateTime.now(),
                    null
            );
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
        log.error("Database root cause: {}", root, root);
        ErrorResponse response = new ErrorResponse(
        	    "Database error: " + (root != null ? root.getMessage() : ex.getMessage()),
        	    HttpStatus.INTERNAL_SERVER_ERROR.value(),
        	    LocalDateTime.now(),
        	    null
        	);
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
    
    @ExceptionHandler(org.springframework.transaction.TransactionSystemException.class)
    public ResponseEntity<ErrorResponse> handleTransactionSystemException(org.springframework.transaction.TransactionSystemException ex) {
        log.error("Transaction error: {}", ex.getMessage(), ex);

        Throwable root = ex.getRootCause();
        if (root instanceof IllegalStateException ise && ise.getMessage().contains("Insufficient funds")) {
            ErrorResponse response = new ErrorResponse(
                    "Insufficient funds: " + ise.getMessage(),
                    HttpStatus.BAD_REQUEST.value(),
                    LocalDateTime.now(),
                    null
            );
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
        
        if (root instanceof javax.validation.ConstraintViolationException cve) {
            Map<String, String> errors = new HashMap<>();
            cve.getConstraintViolations().forEach(violation -> {
                String fieldName = violation.getPropertyPath().toString();
                String errorMessage = violation.getMessage();
                errors.put(fieldName, errorMessage);
            });

            ErrorResponse response = new ErrorResponse(
                    "Validation error: invalid request parameters",
                    HttpStatus.BAD_REQUEST.value(),
                    LocalDateTime.now(),
                    errors
            );
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
        
        log.error("Database root cause: {}", root, root);
        ErrorResponse response = new ErrorResponse(
                "Transaction error",
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                LocalDateTime.now(),
                null
        );
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
    
    // Common catch-all exception
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        log.error("Unhandled exception: {}", ex.getMessage(), ex);

        ErrorResponse response = new ErrorResponse(
                "Internal server error: " + ex.getMessage(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                LocalDateTime.now(),
                null
        );

        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
    
}