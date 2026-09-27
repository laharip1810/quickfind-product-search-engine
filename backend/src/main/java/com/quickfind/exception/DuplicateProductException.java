package com.quickfind.exception;

public class DuplicateProductException extends QuickFindException {

    public DuplicateProductException(String brand, String name) {
        super(ErrorCode.DUPLICATE_PRODUCT, "A " + brand + " product named '" + name + "' already exists");
    }
}
