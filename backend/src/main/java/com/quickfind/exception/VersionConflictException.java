package com.quickfind.exception;

public class VersionConflictException extends QuickFindException {

    public VersionConflictException(Long id, Long expected, Long actual) {
        super(ErrorCode.VERSION_CONFLICT, "Product " + id + " was modified by someone else (your version "
                + expected + ", current version " + actual + "). Reload it and try again.");
    }

    public VersionConflictException(String message) {
        super(ErrorCode.VERSION_CONFLICT, message);
    }
}
