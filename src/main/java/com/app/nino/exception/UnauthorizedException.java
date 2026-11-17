package com.app.nino.exception;

import lombok.Getter;

@Getter
public class UnauthorizedException extends RuntimeException {
    private final String errorCode;

    public UnauthorizedException(String message) {
        super(message);
        this.errorCode = "UNAUTHORIZED";
    }

    public UnauthorizedException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode != null ? errorCode : "UNAUTHORIZED";
    }
}
