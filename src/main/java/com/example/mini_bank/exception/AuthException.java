package com.example.mini_bank.exception;

import org.springframework.http.HttpStatus;

public class AuthException extends RuntimeException {
   
	private static final long serialVersionUID = 1L;
	private final HttpStatus status;

    public AuthException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}