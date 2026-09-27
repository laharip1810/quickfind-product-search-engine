package com.quickfind.exception;

public class InvalidInteractionException extends QuickFindException {

    public InvalidInteractionException(String message) {
        super(ErrorCode.INVALID_REQUEST, message);
    }
}
