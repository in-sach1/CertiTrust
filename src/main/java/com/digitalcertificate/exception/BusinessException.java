package com.digitalcertificate.exception;

/** A rule of the platform was violated (duplicate email, revoking twice, ...). The message is safe to show to users. */
public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}
