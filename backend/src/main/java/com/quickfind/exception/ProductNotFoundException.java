package com.quickfind.exception;

public class ProductNotFoundException extends QuickFindException {

    public ProductNotFoundException(Long id) {
        super(ErrorCode.PRODUCT_NOT_FOUND, "Product " + id + " was not found");
    }
}
