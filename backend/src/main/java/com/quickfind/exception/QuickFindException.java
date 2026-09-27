package com.quickfind.exception;

/** Base class for expected, client-facing errors. The message is safe to show to API users. */
public abstract class QuickFindException extends RuntimeException {

    private final ErrorCode errorCode;

    protected QuickFindException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
