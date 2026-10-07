package com.bcilab.attendance.exception;

/** A valid request that breaks a business rule (inactive person, unknown fingerprint slot, ...). */
public class BusinessRuleException extends RuntimeException {
    public BusinessRuleException(String message) {
        super(message);
    }
}
