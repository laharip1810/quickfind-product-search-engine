package com.quickfind.exception;

public class ResourceNotFoundException extends QuickFindException {

    public ResourceNotFoundException(String resource, Object id) {
        super(ErrorCode.RESOURCE_NOT_FOUND, resource + " " + id + " was not found");
    }
}
