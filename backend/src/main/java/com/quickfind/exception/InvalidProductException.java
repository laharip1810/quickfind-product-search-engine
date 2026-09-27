package com.quickfind.exception;

/** Product data that passes field validation but is semantically wrong (unknown brand, non-leaf category...). */
public class InvalidProductException extends QuickFindException {

    public InvalidProductException(String message) {
        super(ErrorCode.INVALID_PRODUCT, message);
    }
}
