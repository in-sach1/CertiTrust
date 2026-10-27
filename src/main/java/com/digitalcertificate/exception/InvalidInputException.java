package com.digitalcertificate.exception;

/** Incorrect input (bad dates, weak password, ...). A kind of BusinessException, so existing handlers cover it. */
public class InvalidInputException extends BusinessException {
    public InvalidInputException(String message) {
        super(message);
    }
}
